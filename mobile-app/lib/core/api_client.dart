import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import 'realtime_service.dart';

const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api',
);

class ApiClient {
  ApiClient() : _dio = Dio(BaseOptions(baseUrl: apiBaseUrl)) {
    if (kReleaseMode && !apiBaseUrl.startsWith('https://')) {
      throw StateError('Release builds require an HTTPS API_BASE_URL.');
    }
    if (kReleaseMode && !websocketUrl.startsWith('wss://')) {
      throw StateError('Release builds require a secure WSS WEBSOCKET_URL.');
    }
    _dio.interceptors.add(
      InterceptorsWrapper(
        onError: (error, handler) async {
          if (error.response?.statusCode != HttpStatus.unauthorized ||
              _isAuthenticationRequest(error.requestOptions.path)) {
            handler.next(error);
            return;
          }

          if (error.requestOptions.extra['sessionRefreshAttempted'] == true) {
            _onUnauthorized?.call();
            handler.next(error);
            return;
          }

          final refresh = _onRefresh;
          if (refresh == null) {
            _onUnauthorized?.call();
            handler.next(error);
            return;
          }

          try {
            final newToken = await (_refreshing ??= refresh().whenComplete(() {
              _refreshing = null;
            }));
            if (newToken == null) {
              _onUnauthorized?.call();
              handler.next(error);
              return;
            }

            final request = error.requestOptions;
            request.headers[HttpHeaders.authorizationHeader] = 'Bearer $newToken';
            request.extra['sessionRefreshAttempted'] = true;
            handler.resolve(await _dio.fetch<dynamic>(request));
          } on DioException catch (refreshError) {
            if (refreshError.response?.statusCode == HttpStatus.unauthorized) {
              _onUnauthorized?.call();
            }
            handler.next(
              refreshError.response?.statusCode == HttpStatus.unauthorized
                  ? error
                  : refreshError,
            );
          }
        },
      ),
    );
  }

  final Dio _dio;
  VoidCallback? _onUnauthorized;
  Future<String?> Function()? _onRefresh;
  Future<String?>? _refreshing;

  void setUnauthorizedHandler(VoidCallback? handler) {
    _onUnauthorized = handler;
  }

  void setRefreshHandler(Future<String?> Function()? handler) {
    _onRefresh = handler;
  }

  bool _isAuthenticationRequest(String path) =>
      path.endsWith('/auth/login') ||
      path.endsWith('/auth/register') ||
      path.endsWith('/auth/refresh');

  void setToken(String? token) {
    _dio.options.headers.remove(HttpHeaders.authorizationHeader);
    if (token != null && token.isNotEmpty) {
      _dio.options.headers[HttpHeaders.authorizationHeader] = 'Bearer $token';
    }
  }

  Future<Map<String, dynamic>> getMap(String path) async {
    final response = await _dio.get<dynamic>(path);
    return Map<String, dynamic>.from(response.data as Map);
  }

  Future<List<dynamic>> getList(String path) async {
    final response = await _dio.get<dynamic>(path);
    return List<dynamic>.from(response.data as List);
  }

  Future<Map<String, dynamic>> postMap(
    String path, {
    Map<String, dynamic>? data,
  }) async {
    final response = await _dio.post<dynamic>(path, data: data);
    if (response.data == null) {
      return <String, dynamic>{};
    }
    return Map<String, dynamic>.from(response.data as Map);
  }

  Future<Map<String, dynamic>> patchMap(String path) async {
    final response = await _dio.patch<dynamic>(path);
    return Map<String, dynamic>.from(response.data as Map);
  }

  Future<void> delete(String path) async {
    await _dio.delete<void>(path);
  }

  Future<Map<String, dynamic>> uploadQrImage(File file) async {
    final response = await _dio.post<dynamic>(
      '/scans/qr',
      data: FormData.fromMap(<String, dynamic>{
        'file': await MultipartFile.fromFile(file.path),
      }),
    );
    return Map<String, dynamic>.from(response.data as Map);
  }
}

import 'dart:io';

import 'package:dio/dio.dart';

const apiBaseUrl = String.fromEnvironment(
  'API_BASE_URL',
  defaultValue: 'http://10.0.2.2:8080/api',
);

class ApiClient {
  ApiClient() : _dio = Dio(BaseOptions(baseUrl: apiBaseUrl));

  final Dio _dio;

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

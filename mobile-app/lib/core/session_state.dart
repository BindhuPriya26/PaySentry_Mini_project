import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'api_client.dart';
import 'push_notification_service.dart';
import 'realtime_service.dart';
import 'sms_monitoring_service.dart';

class SessionState extends ChangeNotifier {
  SessionState({
    ApiClient? apiClient,
    RealtimeService? realtimeService,
    PushNotificationService? pushNotificationService,
    SmsMonitoringService? smsMonitoringService,
  })  : _apiClient = apiClient ?? ApiClient(),
        _realtimeService = realtimeService ?? RealtimeService(),
        _pushNotificationService =
            pushNotificationService ?? PushNotificationService(),
        _smsMonitoringService = smsMonitoringService ?? SmsMonitoringService() {
    _apiClient.setUnauthorizedHandler(() {
      unawaited(_expireSession());
    });
    _apiClient.setRefreshHandler(_refreshAccessToken);
  }

  static const _tokenKey = 'paysentry_token';
  static const _refreshTokenKey = 'paysentry_refresh_token';
  static const _accountIdKey = 'paysentry_account_id';
  static const _nameKey = 'paysentry_name';
  static const _roleKey = 'paysentry_role';
  static const _smsMonitoringKey = 'paysentry_sms_monitoring_enabled';

  final _storage = const FlutterSecureStorage();
  final ApiClient _apiClient;
  final RealtimeService _realtimeService;
  final PushNotificationService _pushNotificationService;
  final SmsMonitoringService _smsMonitoringService;

  bool isReady = false;
  String? token;
  String? accountId;
  String? fullName;
  String? role;
  Map<String, dynamic>? lastRealtimeAlert;
  int alertVersion = 0;
  bool smsMonitoringEnabled = false;
  Object? smsMonitoringError;
  Timer? _sessionRefreshTimer;

  ApiClient get apiClient => _apiClient;
  bool get isAuthenticated => token != null && accountId != null;

  Future<void> restore() async {
    token = await _storage.read(key: _tokenKey);
    final refreshToken = await _storage.read(key: _refreshTokenKey);
    accountId = await _storage.read(key: _accountIdKey);
    fullName = await _storage.read(key: _nameKey);
    role = await _storage.read(key: _roleKey);
    smsMonitoringEnabled =
        await _storage.read(key: _smsMonitoringKey) == 'true';
    _apiClient.setToken(token);
    if (isAuthenticated && refreshToken != null && refreshToken.isNotEmpty) {
      try {
        final refreshedToken = await _refreshAccessToken();
        if (refreshedToken == null) {
          await _clearLocalSession();
        } else {
          await _restoreAuthenticatedServices();
        }
      } on DioException catch (error) {
        if (error.response?.statusCode == 401) {
          await _clearLocalSession();
        } else {
          await _restoreAuthenticatedServices();
        }
      }
    } else if (isAuthenticated || refreshToken != null) {
      await _clearLocalSession();
    }
    isReady = true;
    notifyListeners();
  }

  Future<void> _restoreAuthenticatedServices() async {
    if (isAuthenticated) {
      _startSessionRefreshTimer();
      _connectRealtime();
      if (smsMonitoringEnabled) {
        if (await _smsMonitoringService.hasPermission()) {
          await _startSmsMonitoring();
        } else {
          smsMonitoringEnabled = false;
          smsMonitoringError =
              StateError('SMS permission was revoked. Enable monitoring again to grant permission.');
          await _storage.delete(key: _smsMonitoringKey);
        }
      }
    }
  }

  Future<void> login(String email, String password) async {
    final response =
        await _apiClient.postMap('/auth/login', data: <String, dynamic>{
      'email': email.trim(),
      'password': password,
    });
    await _acceptAuth(response);
  }

  Future<void> register(String name, String email, String password) async {
    final response =
        await _apiClient.postMap('/auth/register', data: <String, dynamic>{
      'fullName': name.trim(),
      'email': email.trim(),
      'password': password,
    });
    await _acceptAuth(response);
  }

  Future<void> logout() async {
    try {
      await _apiClient.postMap(
        '/auth/logout',
        data: <String, dynamic>{
          'refreshToken': await _storage.read(key: _refreshTokenKey),
        },
      );
    } catch (_) {
      // Local credentials must be cleared even when server revocation is unavailable.
    }
    await _clearLocalSession();
  }

  Future<bool> setSmsMonitoringEnabled(bool enabled) async {
    if (enabled) {
      final permissionGranted = await _smsMonitoringService.requestPermission();
      if (!permissionGranted) {
        return false;
      }
      await _storage.write(key: _smsMonitoringKey, value: 'true');
      smsMonitoringEnabled = true;
      smsMonitoringError = null;
      await _startSmsMonitoring();
    } else {
      await _smsMonitoringService.stop();
      await _storage.delete(key: _smsMonitoringKey);
      smsMonitoringEnabled = false;
      smsMonitoringError = null;
    }
    notifyListeners();
    return true;
  }

  Future<void> _acceptAuth(Map<String, dynamic> response) async {
    token = response['token'] as String?;
    final refreshToken = response['refreshToken'] as String?;
    accountId = response['accountId'] as String?;
    fullName = response['fullName'] as String?;
    role = response['role'] as String?;
    if (!isAuthenticated || refreshToken == null || refreshToken.isEmpty) {
      throw StateError('The server did not return a valid mobile session.');
    }
    _apiClient.setToken(token);
    await _storage.write(key: _tokenKey, value: token);
    await _storage.write(key: _refreshTokenKey, value: refreshToken);
    await _storage.write(key: _accountIdKey, value: accountId);
    await _storage.write(key: _nameKey, value: fullName);
    await _storage.write(key: _roleKey, value: role);
    _startSessionRefreshTimer();
    _connectRealtime();
    await _pushNotificationService.register(_apiClient);
    if (smsMonitoringEnabled) {
      if (await _smsMonitoringService.hasPermission()) {
        await _startSmsMonitoring();
      } else {
        smsMonitoringEnabled = false;
        smsMonitoringError =
            StateError('SMS permission was revoked. Enable monitoring again to grant permission.');
        await _storage.delete(key: _smsMonitoringKey);
      }
    }
    notifyListeners();
  }

  Future<String?> _refreshAccessToken() async {
    final refreshToken = await _storage.read(key: _refreshTokenKey);
    if (refreshToken == null || refreshToken.isEmpty) {
      return null;
    }
    final response = await _apiClient.postMap(
      '/auth/refresh',
      data: <String, dynamic>{'refreshToken': refreshToken},
    );
    final newToken = response['token'] as String?;
    final newRefreshToken = response['refreshToken'] as String?;
    if (newToken == null || newToken.isEmpty ||
        newRefreshToken == null || newRefreshToken.isEmpty) {
      throw StateError('The server did not return a valid refreshed session.');
    }
    token = newToken;
    _apiClient.setToken(newToken);
    await _storage.write(key: _tokenKey, value: newToken);
    await _storage.write(key: _refreshTokenKey, value: newRefreshToken);
    if (isReady && accountId != null) {
      _connectRealtime();
    }
    return newToken;
  }

  void _connectRealtime() {
    final currentAccountId = accountId;
    if (currentAccountId == null) {
      return;
    }
    _realtimeService.connect(
      accountId: currentAccountId,
      token: token!,
      onAlert: (alert) {
        lastRealtimeAlert = alert;
        alertVersion++;
        notifyListeners();
      },
    );
  }

  Future<void> _startSmsMonitoring() async {
    await _smsMonitoringService.start(
      apiClient: _apiClient,
      onError: (error) {
        smsMonitoringError = error;
        notifyListeners();
      },
    );
  }

  Future<void> _expireSession() async {
    if (!isAuthenticated) {
      return;
    }
    await _clearLocalSession();
  }

  Future<void> _clearLocalSession() async {
    _sessionRefreshTimer?.cancel();
    _sessionRefreshTimer = null;
    _realtimeService.disconnect();
    await _smsMonitoringService.stop();
    token = null;
    accountId = null;
    fullName = null;
    role = null;
    lastRealtimeAlert = null;
    smsMonitoringEnabled = false;
    smsMonitoringError = null;
    _apiClient.setToken(null);
    await _storage.deleteAll();
    notifyListeners();
  }

  void _startSessionRefreshTimer() {
    _sessionRefreshTimer?.cancel();
    _sessionRefreshTimer = Timer.periodic(const Duration(minutes: 10), (_) {
      unawaited(_renewSession());
    });
  }

  Future<void> _renewSession() async {
    try {
      await _refreshAccessToken();
    } on DioException catch (error) {
      if (error.response?.statusCode == 401) {
        await _expireSession();
      } else {
        debugPrint('PaySentry session refresh failed; the saved session will be retried.');
      }
    }
  }

  @override
  void dispose() {
    _sessionRefreshTimer?.cancel();
    _realtimeService.disconnect();
    super.dispose();
  }
}

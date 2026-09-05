import 'package:flutter/foundation.dart';
import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'api_client.dart';
import 'push_notification_service.dart';
import 'realtime_service.dart';

class SessionState extends ChangeNotifier {
  SessionState({
    ApiClient? apiClient,
    RealtimeService? realtimeService,
    PushNotificationService? pushNotificationService,
  })  : _apiClient = apiClient ?? ApiClient(),
        _realtimeService = realtimeService ?? RealtimeService(),
        _pushNotificationService = pushNotificationService ?? PushNotificationService();

  static const _tokenKey = 'paysentry_token';
  static const _accountIdKey = 'paysentry_account_id';
  static const _nameKey = 'paysentry_name';
  static const _roleKey = 'paysentry_role';

  final _storage = const FlutterSecureStorage();
  final ApiClient _apiClient;
  final RealtimeService _realtimeService;
  final PushNotificationService _pushNotificationService;

  bool isReady = false;
  String? token;
  String? accountId;
  String? fullName;
  String? role;
  Map<String, dynamic>? lastRealtimeAlert;
  int alertVersion = 0;

  ApiClient get apiClient => _apiClient;
  bool get isAuthenticated => token != null && accountId != null;

  Future<void> restore() async {
    token = await _storage.read(key: _tokenKey);
    accountId = await _storage.read(key: _accountIdKey);
    fullName = await _storage.read(key: _nameKey);
    role = await _storage.read(key: _roleKey);
    _apiClient.setToken(token);
    if (isAuthenticated) {
      _connectRealtime();
    }
    isReady = true;
    notifyListeners();
  }

  Future<void> login(String email, String password) async {
    final response = await _apiClient.postMap('/auth/login', data: <String, dynamic>{
      'email': email.trim(),
      'password': password,
    });
    await _acceptAuth(response);
  }

  Future<void> register(String name, String email, String password) async {
    final response = await _apiClient.postMap('/auth/register', data: <String, dynamic>{
      'fullName': name.trim(),
      'email': email.trim(),
      'password': password,
    });
    await _acceptAuth(response);
  }

  Future<void> logout() async {
    try {
      await _apiClient.postMap('/auth/logout');
    } catch (_) {
      // Local credential removal is still required for a stateless JWT logout.
    }
    _realtimeService.disconnect();
    token = null;
    accountId = null;
    fullName = null;
    role = null;
    lastRealtimeAlert = null;
    _apiClient.setToken(null);
    await _storage.deleteAll();
    notifyListeners();
  }

  Future<void> _acceptAuth(Map<String, dynamic> response) async {
    token = response['token'] as String?;
    accountId = response['accountId'] as String?;
    fullName = response['fullName'] as String?;
    role = response['role'] as String?;
    if (!isAuthenticated) {
      throw StateError('The server did not return a valid mobile session.');
    }
    _apiClient.setToken(token);
    await _storage.write(key: _tokenKey, value: token);
    await _storage.write(key: _accountIdKey, value: accountId);
    await _storage.write(key: _nameKey, value: fullName);
    await _storage.write(key: _roleKey, value: role);
    _connectRealtime();
    await _pushNotificationService.register(_apiClient);
    notifyListeners();
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
}

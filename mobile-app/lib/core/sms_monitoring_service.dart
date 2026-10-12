import 'dart:async';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:receive_sms/receive_sms.dart';

import 'api_client.dart';

class SmsMonitoringService {
  static const MethodChannel _permissionChannel =
      MethodChannel('com.paysentry.mobile/sms_permissions');

  final ReceiveSms _receiveSms = ReceiveSms();
  StreamSubscription<SmsMessage>? _subscription;

  Future<bool> requestPermission() async {
    if (!Platform.isAndroid) {
      return false;
    }
    final result = await _receiveSms.requestPermission();
    return result.granted;
  }

  Future<bool> hasPermission() async {
    if (!Platform.isAndroid) {
      return false;
    }
    return await _permissionChannel.invokeMethod<bool>(
          'hasReceiveSmsPermission',
        ) ??
        false;
  }

  Future<void> start({
    required ApiClient apiClient,
    required void Function(Object error) onError,
  }) async {
    await stop();
    _subscription = _receiveSms.incomingSmsStream.listen(
      (message) {
        final content = message.body.trim();
        if (content.isEmpty) {
          return;
        }
        unawaited(_analyzeMessage(content, apiClient, onError));
      },
      onError: (Object error) => onError(error),
    );
  }

  Future<void> stop() async {
    await _subscription?.cancel();
    _subscription = null;
  }

  Future<void> _analyzeMessage(
    String content,
    ApiClient apiClient,
    void Function(Object error) onError,
  ) async {
    try {
      await apiClient.postMap(
        '/detections/sms',
        data: <String, dynamic>{'content': content, 'consentConfirmed': true},
      );
      for (final url in _extractUrls(content)) {
        await apiClient.postMap(
          '/detections/url',
          data: <String, dynamic>{'content': url},
        );
      }
    } catch (error) {
      onError(error);
    }
  }

  Iterable<String> _extractUrls(String content) {
    final matches = RegExp(
      r'\b(?:https?://|www\.)[^\s<>"\x27]+',
      caseSensitive: false,
    ).allMatches(content);
    return matches
        .map((match) => match.group(0)!)
        .map((url) => url.replaceFirst(RegExp(r'[.,!?;:)\]]+$'), ''))
        .where((url) => url.isNotEmpty)
        .map((url) =>
            url.toLowerCase().startsWith('www.') ? 'https://$url' : url);
  }
}

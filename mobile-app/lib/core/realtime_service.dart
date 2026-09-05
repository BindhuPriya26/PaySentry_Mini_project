import 'dart:convert';

import 'package:stomp_dart_client/stomp_dart_client.dart';

const websocketUrl = String.fromEnvironment(
  'WEBSOCKET_URL',
  defaultValue: 'ws://10.0.2.2:8080/ws',
);

class RealtimeService {
  StompClient? _client;

  void connect({
    required String accountId,
    required String token,
    required void Function(Map<String, dynamic> alert) onAlert,
  }) {
    disconnect();
    _client = StompClient(
      config: StompConfig(
        url: websocketUrl,
        stompConnectHeaders: <String, String>{'Authorization': 'Bearer $token'},
        reconnectDelay: const Duration(seconds: 5),
        heartbeatIncoming: const Duration(seconds: 10),
        heartbeatOutgoing: const Duration(seconds: 10),
        onConnect: (frame) {
          _client?.subscribe(
            destination: '/topic/accounts/$accountId',
            callback: (message) {
              final body = message.body;
              if (body == null) {
                return;
              }
              onAlert(_decode(body));
            },
          );
        },
      ),
    )..activate();
  }

  Map<String, dynamic> _decode(String payload) {
    try {
      return Map<String, dynamic>.from(
        jsonDecode(payload) as Map<String, dynamic>,
      );
    } catch (_) {
      return <String, dynamic>{'title': 'New PaySentry alert', 'message': payload};
    }
  }

  void disconnect() {
    _client?.deactivate();
    _client = null;
  }
}

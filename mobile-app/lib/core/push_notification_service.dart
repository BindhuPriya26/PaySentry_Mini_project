import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';

import 'api_client.dart';

class PushNotificationService {
  Future<void> register(ApiClient apiClient) async {
    try {
      await Firebase.initializeApp();
      final messaging = FirebaseMessaging.instance;
      await messaging.requestPermission(alert: true, badge: true, sound: true);
      final token = await messaging.getToken();
      if (token != null) {
        await apiClient.postMap('/mobile/devices', data: <String, dynamic>{
          'fcmToken': token,
          'deviceName': 'PaySentry mobile app',
        });
      }
    } catch (_) {
      return;
    }
  }
}

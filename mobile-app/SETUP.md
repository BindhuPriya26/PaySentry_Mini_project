# Flutter platform setup

The Android project is included. After installing Flutter and Android Studio/Android SDK, run:

```powershell
Set-Location 'C:\Mini project\PaySentry\mobile-app'
flutter pub get
```

The Android manifest includes camera and internet permissions for QR scanning and API access. The `receive_sms` plugin also declares `SEND_SMS`, which PaySentry does not use; the manifest merger is configured to remove it.

Incoming SMS monitoring is Android-only and opt-in from the app's Profile screen. The app explains the data flow before requesting `RECEIVE_SMS`; startup checks current permission without triggering a new permission prompt. Message text and extracted links are sent to the configured PaySentry API for analysis. Monitoring works while the app process is alive, but Android may terminate it; it is not a guaranteed always-on background service or restart-after-reboot service. Google Play restricts SMS permissions, so confirm distribution eligibility before publishing. This checks SMS received by the device, not WhatsApp messages or every website opened in other apps. QR scanning remains a user-started camera action.

For FCM, register this generated Android application in Firebase, copy `google-services.json` into `android/app/`, then run `flutterfire configure`. The app catches missing Firebase configuration in local development so REST and WebSocket monitoring can still run.

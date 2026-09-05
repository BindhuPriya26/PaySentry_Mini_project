# Flutter platform setup

Run these commands after installing Flutter and Android Studio/Android SDK:

```powershell
Set-Location 'C:\Mini project\PaySentry\mobile-app'
flutter create . --platforms=android
flutter pub get
```

Add the camera permission to `android/app/src/main/AndroidManifest.xml` directly below the `<manifest>` tag:

```xml
<uses-permission android:name="android.permission.CAMERA" />
```

For FCM, register this generated Android application in Firebase, copy `google-services.json` into `android/app/`, then run `flutterfire configure`. The app catches missing Firebase configuration in local development so REST and WebSocket monitoring can still run.

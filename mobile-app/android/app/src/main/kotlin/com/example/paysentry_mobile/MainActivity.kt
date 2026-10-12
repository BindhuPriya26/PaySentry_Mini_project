package com.example.paysentry_mobile

import android.Manifest
import android.os.Build
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)
        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, SMS_PERMISSION_CHANNEL)
            .setMethodCallHandler { call, result ->
                if (call.method != "hasReceiveSmsPermission") {
                    result.notImplemented()
                } else {
                    result.success(
                        Build.VERSION.SDK_INT < Build.VERSION_CODES.M ||
                            checkSelfPermission(Manifest.permission.RECEIVE_SMS) ==
                            android.content.pm.PackageManager.PERMISSION_GRANTED
                    )
                }
            }
    }

    private companion object {
        const val SMS_PERMISSION_CHANNEL = "com.paysentry.mobile/sms_permissions"
    }
}

// This is a basic Flutter widget test.
//
// To perform an interaction with a widget in your test, use the WidgetTester
// utility in the flutter_test package. For example, you can send tap and scroll
// gestures. You can also use WidgetTester to find child widgets in the widget
// tree, read text, and verify that the values of widget properties are correct.

import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:provider/provider.dart';

import 'package:paysentry_mobile/core/session_state.dart';
import 'package:paysentry_mobile/main.dart';

void main() {
  testWidgets('PaySentry shows its startup state', (WidgetTester tester) async {
    final session = SessionState()..isReady = false;
    await tester.pumpWidget(
      ChangeNotifierProvider<SessionState>.value(
        value: session,
        child: const PaySentryApp(),
      ),
    );

    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });
}

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import 'core/session_state.dart';
import 'screens/auth_page.dart';
import 'screens/home_screen.dart';

void main() {
  runApp(
    ChangeNotifierProvider<SessionState>(
      create: (_) => SessionState()..restore(),
      child: const PaySentryApp(),
    ),
  );
}

class PaySentryApp extends StatelessWidget {
  const PaySentryApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'PaySentry',
      debugShowCheckedModeBanner: false,
      theme: ThemeData(
        colorScheme: ColorScheme.fromSeed(
          seedColor: const Color(0xff0c3b68),
          brightness: Brightness.light,
        ),
        useMaterial3: true,
      ),
      home: Consumer<SessionState>(
        builder: (context, session, _) {
          if (!session.isReady) {
            return const Scaffold(body: Center(child: CircularProgressIndicator()));
          }
          return session.isAuthenticated ? const HomeScreen() : const AuthPage();
        },
      ),
    );
  }
}

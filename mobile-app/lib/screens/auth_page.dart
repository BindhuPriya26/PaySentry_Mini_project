import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../core/session_state.dart';

class AuthPage extends StatefulWidget {
  const AuthPage({super.key});

  @override
  State<AuthPage> createState() => _AuthPageState();
}

class _AuthPageState extends State<AuthPage> with SingleTickerProviderStateMixin {
  late final TabController _tabController;
  final _loginFormKey = GlobalKey<FormState>();
  final _registerFormKey = GlobalKey<FormState>();
  final _loginEmail = TextEditingController();
  final _loginPassword = TextEditingController();
  final _name = TextEditingController();
  final _registerEmail = TextEditingController();
  final _registerPassword = TextEditingController();
  bool _loading = false;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(length: 2, vsync: this);
  }

  @override
  void dispose() {
    _tabController.dispose();
    _loginEmail.dispose();
    _loginPassword.dispose();
    _name.dispose();
    _registerEmail.dispose();
    _registerPassword.dispose();
    super.dispose();
  }

  Future<void> _submitLogin() async {
    if (!_loginFormKey.currentState!.validate()) {
      return;
    }
    await _run(() => context.read<SessionState>().login(_loginEmail.text, _loginPassword.text));
  }

  Future<void> _submitRegistration() async {
    if (!_registerFormKey.currentState!.validate()) {
      return;
    }
    await _run(() => context.read<SessionState>().register(
          _name.text,
          _registerEmail.text,
          _registerPassword.text,
        ));
  }

  Future<void> _run(Future<void> Function() action) async {
    setState(() => _loading = true);
    try {
      await action();
    } catch (error) {
      if (!mounted) {
        return;
      }
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_messageFor(error))));
    } finally {
      if (mounted) {
        setState(() => _loading = false);
      }
    }
  }

  String _messageFor(Object error) {
    if (error is DioException && error.response?.data is Map) {
      return (error.response!.data as Map)['message'] as String? ?? 'Unable to sign in.';
    }
    return error.toString().replaceFirst('Exception: ', '');
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 480),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(24),
                  child: Column(
                    children: <Widget>[
                      const Icon(Icons.shield_outlined, size: 56),
                      const SizedBox(height: 12),
                      Text('PaySentry', style: Theme.of(context).textTheme.headlineMedium),
                      const Text('Your real-time digital banking safety monitor'),
                      const SizedBox(height: 24),
                      TabBar(controller: _tabController, tabs: const <Tab>[
                        Tab(text: 'Sign in'),
                        Tab(text: 'Register'),
                      ]),
                      SizedBox(
                        height: 330,
                        child: TabBarView(
                          controller: _tabController,
                          children: <Widget>[
                            _loginForm(),
                            _registerForm(),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _loginForm() {
    return Form(
      key: _loginFormKey,
      child: Column(
        children: <Widget>[
          const SizedBox(height: 20),
          TextFormField(
            controller: _loginEmail,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(labelText: 'Email'),
            validator: _required,
          ),
          TextFormField(
            controller: _loginPassword,
            obscureText: true,
            decoration: const InputDecoration(labelText: 'Password'),
            validator: _required,
          ),
          const Spacer(),
          SizedBox(width: double.infinity, child: FilledButton(
            onPressed: _loading ? null : _submitLogin,
            child: Text(_loading ? 'Signing in...' : 'Sign in'),
          )),
        ],
      ),
    );
  }

  Widget _registerForm() {
    return Form(
      key: _registerFormKey,
      child: Column(
        children: <Widget>[
          const SizedBox(height: 20),
          TextFormField(controller: _name, decoration: const InputDecoration(labelText: 'Full name'), validator: _required),
          TextFormField(controller: _registerEmail, keyboardType: TextInputType.emailAddress, decoration: const InputDecoration(labelText: 'Email'), validator: _required),
          TextFormField(controller: _registerPassword, obscureText: true, decoration: const InputDecoration(labelText: 'Password'), validator: (value) {
            if (value == null || value.length < 8) return 'Use at least 8 characters.';
            return null;
          }),
          const Spacer(),
          SizedBox(width: double.infinity, child: FilledButton(
            onPressed: _loading ? null : _submitRegistration,
            child: Text(_loading ? 'Creating account...' : 'Create protected account'),
          )),
        ],
      ),
    );
  }

  String? _required(String? value) => value == null || value.trim().isEmpty ? 'This field is required.' : null;
}

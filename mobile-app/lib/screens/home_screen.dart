import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:image_picker/image_picker.dart';
import 'package:intl/intl.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:provider/provider.dart';

import '../core/api_client.dart';
import '../core/session_state.dart';
import '../models/dashboard.dart';

class HomeScreen extends StatefulWidget {
  const HomeScreen({super.key});

  @override
  State<HomeScreen> createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  int _selectedIndex = 0;

  @override
  Widget build(BuildContext context) {
    return Consumer<SessionState>(
      builder: (context, session, _) {
        final pages = <Widget>[
          DashboardPage(key: ValueKey(session.alertVersion), apiClient: session.apiClient),
          DetectionPage(apiClient: session.apiClient),
          AlertsPage(key: ValueKey(session.alertVersion), apiClient: session.apiClient),
          ProfilePage(session: session),
        ];
        return Scaffold(
          appBar: AppBar(
            title: const Text('PaySentry'),
            actions: <Widget>[
              if (session.lastRealtimeAlert != null)
                IconButton(
                  tooltip: 'Latest real-time alert',
                  icon: const Icon(Icons.notifications_active_outlined),
                  onPressed: () => _showRealtimeAlert(session.lastRealtimeAlert!),
                ),
            ],
          ),
          body: IndexedStack(index: _selectedIndex, children: pages),
          bottomNavigationBar: NavigationBar(
            selectedIndex: _selectedIndex,
            onDestinationSelected: (index) => setState(() => _selectedIndex = index),
            destinations: const <NavigationDestination>[
              NavigationDestination(icon: Icon(Icons.dashboard_outlined), selectedIcon: Icon(Icons.dashboard), label: 'Dashboard'),
              NavigationDestination(icon: Icon(Icons.security_outlined), selectedIcon: Icon(Icons.security), label: 'Scan'),
              NavigationDestination(icon: Icon(Icons.warning_amber_outlined), selectedIcon: Icon(Icons.warning_amber), label: 'Alerts'),
              NavigationDestination(icon: Icon(Icons.person_outline), selectedIcon: Icon(Icons.person), label: 'Profile'),
            ],
          ),
        );
      },
    );
  }

  void _showRealtimeAlert(Map<String, dynamic> alert) {
    showDialog<void>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(alert['title'] as String? ?? 'New PaySentry alert'),
        content: Text('Risk score: ${alert['riskScore'] ?? 'unknown'}/100\n${alert['recommendedAction'] ?? ''}'),
        actions: <Widget>[TextButton(onPressed: () => Navigator.pop(context), child: const Text('Review'))],
      ),
    );
  }
}

class DashboardPage extends StatefulWidget {
  const DashboardPage({super.key, required this.apiClient});

  final ApiClient apiClient;

  @override
  State<DashboardPage> createState() => _DashboardPageState();
}

class _DashboardPageState extends State<DashboardPage> {
  late Future<DashboardData> _dashboard;

  @override
  void initState() {
    super.initState();
    _dashboard = _load();
  }

  Future<DashboardData> _load() async => DashboardData.fromJson(
        await widget.apiClient.getMap('/mobile/dashboard'),
      );

  Future<void> _refresh() async {
    setState(() => _dashboard = _load());
    await _dashboard;
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<DashboardData>(
      future: _dashboard,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) {
          return const Center(child: CircularProgressIndicator());
        }
        if (snapshot.hasError) {
          return _ErrorState(message: _messageFor(snapshot.error), onRetry: _refresh);
        }
        final dashboard = snapshot.requireData;
        return RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            physics: const AlwaysScrollableScrollPhysics(),
            padding: const EdgeInsets.all(16),
            children: <Widget>[
              _ProtectionCard(dashboard: dashboard),
              const SizedBox(height: 16),
              _BalanceGrid(dashboard: dashboard),
              const SizedBox(height: 24),
              Text('Recent transactions', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              if (dashboard.transactions.isEmpty)
                const _EmptyCard(message: 'No mock-bank transactions have been received yet.')
              else
                ...dashboard.transactions.map(_TransactionTile.new),
              const SizedBox(height: 24),
              Text('Recent alerts', style: Theme.of(context).textTheme.titleLarge),
              const SizedBox(height: 8),
              if (dashboard.alerts.isEmpty)
                const _EmptyCard(message: 'Protection is active. No suspicious events detected.')
              else
                ...dashboard.alerts.map(_AlertTile.new),
            ],
          ),
        );
      },
    );
  }
}

class DetectionPage extends StatefulWidget {
  const DetectionPage({super.key, required this.apiClient});

  final ApiClient apiClient;

  @override
  State<DetectionPage> createState() => _DetectionPageState();
}

class _DetectionPageState extends State<DetectionPage> {
  final _controller = TextEditingController();
  String _mode = 'SMS';
  Map<String, dynamic>? _result;
  bool _loading = false;

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  Future<void> _analyzeText() async {
    if (_controller.text.trim().isEmpty) {
      return;
    }
    setState(() => _loading = true);
    try {
      final path = _mode == 'SMS' ? '/detections/sms' : '/detections/url';
      final result = await widget.apiClient.postMap(path, data: <String, dynamic>{'content': _controller.text.trim()});
      setState(() => _result = result);
    } catch (error) {
      _showError(error);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _scanQr() async {
    final content = await Navigator.of(context).push<String>(MaterialPageRoute<String>(
      builder: (_) => const QrCameraPage(),
    ));
    if (content == null || !mounted) {
      return;
    }
    await _analyzeQrContent(content);
  }

  Future<void> _analyzeQrContent(String content) async {
    setState(() => _loading = true);
    try {
      final result = await widget.apiClient.postMap('/detections/qr', data: <String, dynamic>{'content': content});
      setState(() => _result = result);
    } catch (error) {
      _showError(error);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _uploadQr() async {
    final image = await ImagePicker().pickImage(source: ImageSource.gallery);
    if (image == null) {
      return;
    }
    setState(() => _loading = true);
    try {
      final result = await widget.apiClient.uploadQrImage(File(image.path));
      setState(() => _result = result);
    } catch (error) {
      _showError(error);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _showError(Object error) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(_messageFor(error))));
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        Text('Fraud checks', style: Theme.of(context).textTheme.headlineSmall),
        const SizedBox(height: 8),
        const Text('Each result is sent to the unified risk engine and correlated with recent banking activity.'),
        const SizedBox(height: 16),
        SegmentedButton<String>(
          segments: const <ButtonSegment<String>>[
            ButtonSegment<String>(value: 'SMS', label: Text('SMS'), icon: Icon(Icons.sms_outlined)),
            ButtonSegment<String>(value: 'URL', label: Text('URL'), icon: Icon(Icons.link)),
            ButtonSegment<String>(value: 'QR', label: Text('QR'), icon: Icon(Icons.qr_code_scanner)),
          ],
          selected: <String>{_mode},
          onSelectionChanged: (value) => setState(() {
            _mode = value.first;
            _result = null;
          }),
        ),
        const SizedBox(height: 16),
        if (_mode == 'QR') ...<Widget>[
          FilledButton.icon(onPressed: _loading ? null : _scanQr, icon: const Icon(Icons.camera_alt_outlined), label: const Text('Scan QR with camera')),
          const SizedBox(height: 8),
          OutlinedButton.icon(onPressed: _loading ? null : _uploadQr, icon: const Icon(Icons.upload_file_outlined), label: const Text('Upload QR image')),
        ] else ...<Widget>[
          TextField(
            controller: _controller,
            minLines: _mode == 'SMS' ? 4 : 1,
            maxLines: _mode == 'SMS' ? 7 : 1,
            keyboardType: _mode == 'URL' ? TextInputType.url : TextInputType.multiline,
            decoration: InputDecoration(
              labelText: _mode == 'SMS' ? 'Paste suspicious message' : 'Enter URL to inspect',
              border: const OutlineInputBorder(),
            ),
          ),
          const SizedBox(height: 12),
          FilledButton.icon(onPressed: _loading ? null : _analyzeText, icon: const Icon(Icons.shield_outlined), label: Text(_loading ? 'Analyzing...' : 'Analyze $_mode')),
        ],
        if (_loading) const Padding(padding: EdgeInsets.all(20), child: Center(child: CircularProgressIndicator())),
        if (_result != null) Padding(padding: const EdgeInsets.only(top: 20), child: _DetectionResult(result: _result!)),
      ],
    );
  }
}

class AlertsPage extends StatefulWidget {
  const AlertsPage({super.key, required this.apiClient});

  final ApiClient apiClient;

  @override
  State<AlertsPage> createState() => _AlertsPageState();
}

class _AlertsPageState extends State<AlertsPage> {
  late Future<List<dynamic>> _alerts;

  @override
  void initState() {
    super.initState();
    _alerts = widget.apiClient.getList('/mobile/alerts');
  }

  Future<void> _refresh() async {
    setState(() => _alerts = widget.apiClient.getList('/mobile/alerts'));
    await _alerts;
  }

  Future<void> _acknowledge(Map<String, dynamic> alert) async {
    await widget.apiClient.patchMap('/mobile/alerts/${alert['id']}/acknowledge');
    await _refresh();
  }

  @override
  Widget build(BuildContext context) {
    return FutureBuilder<List<dynamic>>(
      future: _alerts,
      builder: (context, snapshot) {
        if (snapshot.connectionState != ConnectionState.done) return const Center(child: CircularProgressIndicator());
        if (snapshot.hasError) return _ErrorState(message: _messageFor(snapshot.error), onRetry: _refresh);
        final alerts = snapshot.requireData.map((item) => Map<String, dynamic>.from(item as Map)).toList();
        return RefreshIndicator(
          onRefresh: _refresh,
          child: ListView(
            padding: const EdgeInsets.all(16),
            children: <Widget>[
              Text('Fraud history', style: Theme.of(context).textTheme.headlineSmall),
              const SizedBox(height: 8),
              const Text('Acknowledging an alert only records your review. It never changes a bank lien or transaction.'),
              const SizedBox(height: 12),
              if (alerts.isEmpty)
                const _EmptyCard(message: 'No fraud alerts yet.')
              else
                ...alerts.map((alert) => Card(
                  child: ListTile(
                    leading: _RiskIcon(level: alert['riskLevel'] as String? ?? 'LOW'),
                    title: Text(alert['title'] as String? ?? 'Security alert'),
                    subtitle: Text('Score ${alert['riskScore']}/100\n${alert['recommendedAction'] ?? ''}'),
                    isThreeLine: true,
                    trailing: alert['status'] == 'ACKNOWLEDGED'
                        ? const Icon(Icons.check_circle_outline)
                        : TextButton(onPressed: () => _acknowledge(alert), child: const Text('Acknowledge')),
                  ),
                )),
            ],
          ),
        );
      },
    );
  }
}

class ProfilePage extends StatelessWidget {
  const ProfilePage({super.key, required this.session});

  final SessionState session;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: <Widget>[
        CircleAvatar(radius: 36, child: Text((session.fullName ?? 'U').substring(0, 1).toUpperCase())),
        const SizedBox(height: 12),
        Center(child: Text(session.fullName ?? 'PaySentry user', style: Theme.of(context).textTheme.headlineSmall)),
        Center(child: Text(session.role ?? 'USER')),
        const SizedBox(height: 24),
        Card(child: ListTile(leading: const Icon(Icons.account_balance_outlined), title: const Text('Monitored account'), subtitle: Text(session.accountId ?? ''))),
        Card(child: ListTile(leading: const Icon(Icons.notifications_active_outlined), title: const Text('Push notifications'), subtitle: const Text('Enabled when Firebase is configured for this build.'))),
        const SizedBox(height: 16),
        FilledButton.tonalIcon(
          onPressed: () => session.logout(),
          icon: const Icon(Icons.logout),
          label: const Text('Log out'),
        ),
      ],
    );
  }
}

class QrCameraPage extends StatelessWidget {
  const QrCameraPage({super.key});

  @override
  Widget build(BuildContext context) {
    var handled = false;
    return Scaffold(
      appBar: AppBar(title: const Text('Scan QR code')),
      body: MobileScanner(
        onDetect: (capture) {
          if (handled) return;
          if (capture.barcodes.isEmpty) return;
          final value = capture.barcodes.first.rawValue;
          if (value != null) {
            handled = true;
            Navigator.pop(context, value);
          }
        },
      ),
    );
  }
}

class _ProtectionCard extends StatelessWidget {
  const _ProtectionCard({required this.dashboard});
  final DashboardData dashboard;

  @override
  Widget build(BuildContext context) {
    final risk = 100 - dashboard.securityScore;
    return Card(
      color: _riskColor(risk).withValues(alpha: 0.13),
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Row(
          children: <Widget>[
            CircleAvatar(radius: 30, backgroundColor: _riskColor(risk), child: const Icon(Icons.shield, color: Colors.white)),
            const SizedBox(width: 16),
            Expanded(child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: <Widget>[
              Text(dashboard.protectionStatus, style: Theme.of(context).textTheme.titleMedium),
              Text('Security score ${dashboard.securityScore}/100 · ${dashboard.accountId}'),
            ])),
          ],
        ),
      ),
    );
  }
}

class _BalanceGrid extends StatelessWidget {
  const _BalanceGrid({required this.dashboard});
  final DashboardData dashboard;

  @override
  Widget build(BuildContext context) {
    return GridView.count(
      crossAxisCount: 2,
      shrinkWrap: true,
      childAspectRatio: 1.7,
      physics: const NeverScrollableScrollPhysics(),
      children: <Widget>[
        _BalanceCard(label: 'Total balance', amount: dashboard.totalBalance),
        _BalanceCard(label: 'Available', amount: dashboard.availableBalance),
        _BalanceCard(label: 'Current lien', amount: dashboard.lienAmount, warning: true),
        _BalanceCard(label: 'Security score', amount: dashboard.securityScore.toDouble(), isScore: true),
      ],
    );
  }
}

class _BalanceCard extends StatelessWidget {
  const _BalanceCard({required this.label, required this.amount, this.warning = false, this.isScore = false});
  final String label;
  final double amount;
  final bool warning;
  final bool isScore;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisAlignment: MainAxisAlignment.center, children: <Widget>[
          Text(label, style: Theme.of(context).textTheme.labelLarge),
          const SizedBox(height: 4),
          Text(isScore ? '${amount.round()}/100' : _currency(amount), style: Theme.of(context).textTheme.titleMedium?.copyWith(color: warning ? Colors.deepOrange : null)),
        ]),
      ),
    );
  }
}

class _TransactionTile extends StatelessWidget {
  const _TransactionTile(this.transaction);
  final Map<String, dynamic> transaction;

  @override
  Widget build(BuildContext context) {
    return Card(child: ListTile(
      leading: _RiskIcon(level: transaction['riskLevel'] as String? ?? 'LOW'),
      title: Text(transaction['receiver'] as String? ?? 'Transaction'),
      subtitle: Text('${transaction['location'] ?? 'Unknown location'} · ${transaction['device'] ?? 'Unknown device'}'),
      trailing: Column(mainAxisAlignment: MainAxisAlignment.center, crossAxisAlignment: CrossAxisAlignment.end, children: <Widget>[
        Text(_currency((transaction['amount'] as num?)?.toDouble() ?? 0)),
        Text('Risk ${transaction['riskScore'] ?? 0}', style: Theme.of(context).textTheme.labelSmall),
      ]),
    ));
  }
}

class _AlertTile extends StatelessWidget {
  const _AlertTile(this.alert);
  final Map<String, dynamic> alert;

  @override
  Widget build(BuildContext context) => Card(child: ListTile(
    leading: _RiskIcon(level: alert['riskLevel'] as String? ?? 'LOW'),
    title: Text(alert['title'] as String? ?? 'Security alert'),
    subtitle: Text('Score ${alert['riskScore'] ?? 0}/100'),
  ));
}

class _DetectionResult extends StatelessWidget {
  const _DetectionResult({required this.result});
  final Map<String, dynamic> result;

  @override
  Widget build(BuildContext context) {
    final items = (result['reasons'] ?? result['findings'] ?? <dynamic>[]) as List<dynamic>;
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: <Widget>[
          Row(children: <Widget>[
            _RiskIcon(level: result['riskLevel'] as String? ?? 'LOW'),
            const SizedBox(width: 8),
            Text('${result['riskLevel'] ?? 'LOW'} · ${result['riskScore'] ?? 0}/100', style: Theme.of(context).textTheme.titleMedium),
          ]),
          const SizedBox(height:12),
          ...items.map((item) => Padding(padding: const EdgeInsets.only(bottom: 5), child: Text('• $item'))),
          if (result['recommendedAction'] != null) ...<Widget>[
            const SizedBox(height: 8),
            Text('Recommended action', style: Theme.of(context).textTheme.titleSmall),
            Text(result['recommendedAction'] as String),
          ],
        ]),
      ),
    );
  }
}

class _RiskIcon extends StatelessWidget {
  const _RiskIcon({required this.level});
  final String level;
  @override
  Widget build(BuildContext context) => CircleAvatar(backgroundColor: _riskColor(_riskFor(level)), child: const Icon(Icons.shield, color: Colors.white));
}

class _EmptyCard extends StatelessWidget {
  const _EmptyCard({required this.message});
  final String message;
  @override
  Widget build(BuildContext context) => Card(child: Padding(padding: const EdgeInsets.all(16), child: Text(message)));
}

class _ErrorState extends StatelessWidget {
  const _ErrorState({required this.message, required this.onRetry});
  final String message;
  final Future<void> Function() onRetry;
  @override
  Widget build(BuildContext context) => Center(child: Padding(padding: const EdgeInsets.all(24), child: Column(mainAxisSize: MainAxisSize.min, children: <Widget>[
    const Icon(Icons.cloud_off_outlined, size: 48), const SizedBox(height: 12), Text(message, textAlign: TextAlign.center), const SizedBox(height: 12), FilledButton(onPressed: onRetry, child: const Text('Retry')),
  ])));
}

String _currency(double amount) => NumberFormat.currency(locale: 'en_IN', symbol: '₹', decimalDigits: 2).format(amount);
int _riskFor(String level) => switch (level) { 'CRITICAL' => 100, 'HIGH' => 80, 'MEDIUM' => 50, _ => 10 };
Color _riskColor(int risk) => risk >= 91 ? Colors.red : risk >= 71 ? Colors.deepOrange : risk >= 31 ? Colors.amber.shade800 : Colors.green;
String _messageFor(Object? error) {
  if (error is DioException && error.response?.data is Map) return (error.response!.data as Map)['message'] as String? ?? 'Request failed.';
  return error?.toString().replaceFirst('Exception: ', '') ?? 'Request failed.';
}

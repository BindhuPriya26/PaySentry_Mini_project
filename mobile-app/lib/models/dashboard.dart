class DashboardData {
  DashboardData({
    required this.accountId,
    required this.totalBalance,
    required this.availableBalance,
    required this.lienAmount,
    required this.currency,
    required this.securityScore,
    required this.protectionStatus,
    required this.transactions,
    required this.alerts,
  });

  final String accountId;
  final double totalBalance;
  final double availableBalance;
  final double lienAmount;
  final String currency;
  final int securityScore;
  final String protectionStatus;
  final List<Map<String, dynamic>> transactions;
  final List<Map<String, dynamic>> alerts;

  factory DashboardData.fromJson(Map<String, dynamic> json) {
    return DashboardData(
      accountId: json['accountId'] as String? ?? '',
      totalBalance: _number(json['totalBalance']),
      availableBalance: _number(json['availableBalance']),
      lienAmount: _number(json['lienAmount']),
      currency: json['currency'] as String? ?? 'INR',
      securityScore: (json['securityScore'] as num?)?.toInt() ?? 100,
      protectionStatus: json['protectionStatus'] as String? ?? 'PROTECTION ACTIVE',
      transactions: _items(json['recentTransactions']),
      alerts: _items(json['recentAlerts']),
    );
  }

  static double _number(dynamic value) => (value as num?)?.toDouble() ?? 0;

  static List<Map<String, dynamic>> _items(dynamic value) =>
      (value as List<dynamic>? ?? <dynamic>[])
          .map((item) => Map<String, dynamic>.from(item as Map))
          .toList();
}

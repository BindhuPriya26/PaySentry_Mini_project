import { describeRiskScore, firstName, formatDate } from "../lib/helpers";

function DashboardHeader({ dashboard, lastSyncedAt, loading, notice, onRefresh, user }) {
  const averageRiskScore = Math.round(dashboard?.averageRiskScore ?? 0);
  const fraudAlerts = dashboard?.fraudAlerts ?? 0;
  const totalFraudScans = dashboard?.totalFraudScans ?? 0;

  return (
    <header className="workspace-header">
      <div>
        <span className="eyebrow">Fraud command center</span>
        <h1>{firstName(user.fullName)}, your PaySentry workspace is ready.</h1>
        <p>
          Track fraud exposure across digital banking channels, capture suspicious activity, and
          move from detection to action in one place.
        </p>

        <div className="header-mini-grid">
          <div className="mini-stat">
            <strong>{fraudAlerts}</strong>
            <span>Open alerts</span>
          </div>
          <div className="mini-stat">
            <strong>{totalFraudScans}</strong>
            <span>Total scans saved</span>
          </div>
          <div className="mini-stat">
            <strong>{lastSyncedAt ? formatDate(lastSyncedAt) : "Waiting"}</strong>
            <span>Last sync</span>
          </div>
        </div>
      </div>

      <div className="header-actions">
        <div className="pulse-card">
          <span>Current average risk</span>
          <strong>{averageRiskScore}</strong>
          <small>{loading ? "Refreshing metrics..." : describeRiskScore(averageRiskScore)}</small>
        </div>

        <button className="secondary-button" disabled={loading} onClick={onRefresh} type="button">
          {loading ? "Refreshing..." : "Refresh workspace"}
        </button>
      </div>

      {notice && (
        <div className={`notice inline ${notice.tone}`}>
          <strong>{notice.title}</strong>
          <span>{notice.message}</span>
        </div>
      )}
    </header>
  );
}

export default DashboardHeader;

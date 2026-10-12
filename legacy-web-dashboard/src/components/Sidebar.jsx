import { describeRiskScore } from "../lib/helpers";

function Sidebar({ activeView, dashboard, navItems, onLogout, onSelectView, user, unifiedAlerts }) {
  const averageRisk = unifiedAlerts.length
    ? Math.max(...unifiedAlerts.map((alert) => alert.riskScore))
    : 0;
  const fraudAlerts = unifiedAlerts.filter((alert) => alert.status !== "ACKNOWLEDGED").length;

  return (
    <aside className="sidebar">
      <div className="brand-block">
        <div className="brand-mark">PS</div>
        <div>
          <h2>PaySentry</h2>
          <p>Unified fraud intelligence</p>
        </div>
      </div>

      <div className="profile-card">
        <span className="profile-chip">{user.role}</span>
        <strong>{user.fullName}</strong>
        <small>{user.email}</small>
      </div>

      <div className="status-card">
        <span className="eyebrow">Workspace posture</span>
        <strong>{fraudAlerts > 0 ? `${fraudAlerts} alerts need review` : "No active alerts"}</strong>
        <small>{describeRiskScore(averageRisk)}</small>
      </div>

      <nav className="nav-list">
        {navItems.map((item) => (
          <button
            key={item.id}
            className={activeView === item.id ? "nav-item active" : "nav-item"}
            onClick={() => onSelectView(item.id)}
            type="button"
          >
            <span>{item.label}</span>
          </button>
        ))}
      </nav>

      <button className="ghost-button logout-button" onClick={onLogout} type="button">
        Sign out
      </button>
    </aside>
  );
}

export default Sidebar;

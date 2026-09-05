import { useDeferredValue, useState } from "react";

import {
  formatDate,
  formatRiskLabel,
  formatScanTypeLabel,
  riskTone,
  truncateText
} from "../lib/helpers";

function AdminSection({ adminReports, stats, togglingUserId, users, onDownload, onToggleUser }) {
  const [query, setQuery] = useState("");
  const deferredQuery = useDeferredValue(query);
  const normalizedQuery = deferredQuery.trim().toLowerCase();

  const statCards = [
    { label: "Total users", value: stats?.totalUsers ?? 0 },
    { label: "Active users", value: stats?.activeUsers ?? 0 },
    { label: "Total reports", value: stats?.totalReports ?? 0 },
    { label: "High risk reports", value: stats?.highRiskReports ?? 0 },
    { label: "SMS scans", value: stats?.totalSmsScans ?? 0 },
    { label: "QR scans", value: stats?.totalQrScans ?? 0 },
    { label: "URL scans", value: stats?.totalUrlScans ?? 0 },
    { label: "Transactions", value: stats?.totalTransactions ?? 0 }
  ];

  const filteredUsers = users.filter((user) => {
    const haystack = `${user.fullName} ${user.email} ${user.role}`.toLowerCase();
    return haystack.includes(normalizedQuery);
  });

  return (
    <section className="section-stack">
      <div className="card-grid summary admin-summary">
        {statCards.map((card) => (
          <article className="metric-card" key={card.label}>
            <span>{card.label}</span>
            <strong>{card.value}</strong>
          </article>
        ))}
      </div>

      <article className="panel-card">
        <div className="panel-heading spread">
          <div>
            <span className="eyebrow">User management</span>
            <h3>Access and account controls</h3>
            <p>Review account status, last login activity, and temporarily suspend risky access.</p>
          </div>

          <label className="field compact">
            <span>Find a user</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by name, email, or role"
              type="search"
              value={query}
            />
          </label>
        </div>

        {filteredUsers.length === 0 ? (
          <div className="empty-state">No users match the current search.</div>
        ) : (
          <div className="table-shell">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Last login</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {filteredUsers.map((user) => (
                  <tr key={user.id}>
                    <td>{user.fullName}</td>
                    <td>{user.email}</td>
                    <td>{user.role}</td>
                    <td>
                      <span className={`risk-pill ${user.active ? "safe" : "high"}`}>
                        {user.active ? "Active" : "Disabled"}
                      </span>
                    </td>
                    <td>{formatDate(user.createdAt)}</td>
                    <td>{formatDate(user.lastLoginAt)}</td>
                    <td>
                      <button
                        className="ghost-button"
                        disabled={togglingUserId === user.id}
                        onClick={() => onToggleUser(user.id, !user.active)}
                        type="button"
                      >
                        {togglingUserId === user.id
                          ? "Updating..."
                          : user.active
                            ? "Disable"
                            : "Enable"}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </article>

      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">System reports</span>
            <h3>High-level fraud history</h3>
            <p>Export saved reports for incident review, auditing, or compliance handoff.</p>
          </div>
        </div>

        {adminReports.length === 0 ? (
          <div className="empty-state">No system reports have been saved yet.</div>
        ) : (
          <div className="table-shell">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Owner</th>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Risk</th>
                  <th>Created</th>
                  <th>Summary</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {adminReports.map((report) => (
                  <tr key={report.id}>
                    <td>{report.ownerName}</td>
                    <td>{report.title}</td>
                    <td>{formatScanTypeLabel(report.scanType)}</td>
                    <td>
                      <span className={`risk-pill ${riskTone(report.riskLevel)}`}>
                        {formatRiskLabel(report.riskLevel)}
                      </span>
                    </td>
                    <td>{formatDate(report.createdAt)}</td>
                    <td>{truncateText(report.summary, 95)}</td>
                    <td>
                      <button className="ghost-button" onClick={() => onDownload(report.id)} type="button">
                        Download PDF
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </article>
    </section>
  );
}

export default AdminSection;

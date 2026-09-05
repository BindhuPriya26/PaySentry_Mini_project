import { useDeferredValue, useState } from "react";

import { formatDate, formatRiskLabel, formatScanTypeLabel, riskTone, truncateText } from "../lib/helpers";

function ReportsSection({ downloadingId, reports, onDownload }) {
  const [query, setQuery] = useState("");
  const deferredQuery = useDeferredValue(query);
  const normalizedQuery = deferredQuery.trim().toLowerCase();

  const totalReports = reports.length;
  const highRiskReports = reports.filter((report) => report.riskLevel === "HIGH_RISK").length;
  const recentReports = reports.filter((report) => {
    if (!report.createdAt) {
      return false;
    }

    const reportDate = new Date(report.createdAt).getTime();
    const sevenDaysAgo = Date.now() - 7 * 24 * 60 * 60 * 1000;
    return reportDate >= sevenDaysAgo;
  }).length;

  const filteredReports = reports.filter((report) => {
    const haystack = `${report.title} ${report.scanType} ${report.summary}`.toLowerCase();
    return haystack.includes(normalizedQuery);
  });

  return (
    <section className="section-stack">
      <div className="card-grid summary">
        <article className="metric-card">
          <span>Total reports</span>
          <strong>{totalReports}</strong>
          <small>Saved across all scan channels</small>
        </article>
        <article className="metric-card">
          <span>High-risk reports</span>
          <strong>{highRiskReports}</strong>
          <small>Require the fastest follow-up</small>
        </article>
        <article className="metric-card">
          <span>Last 7 days</span>
          <strong>{recentReports}</strong>
          <small>Recently generated cases</small>
        </article>
      </div>

      <article className="panel-card">
        <div className="panel-heading spread">
          <div>
            <span className="eyebrow">Saved intelligence</span>
            <h3>Reports and export center</h3>
            <p>Search past investigations, review risk levels, and export shareable PDF reports.</p>
          </div>

          <label className="field compact">
            <span>Filter reports</span>
            <input
              onChange={(event) => setQuery(event.target.value)}
              placeholder="Search by title, type, or summary"
              type="search"
              value={query}
            />
          </label>
        </div>

        {filteredReports.length === 0 && (
          <div className="empty-state">
            No reports match the current filter. Run scans or clear the search to see saved cases.
          </div>
        )}

        {filteredReports.length > 0 && (
          <div className="table-shell">
            <table className="data-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Risk</th>
                  <th>Created</th>
                  <th>Summary</th>
                  <th />
                </tr>
              </thead>
              <tbody>
                {filteredReports.map((report) => (
                  <tr key={report.id}>
                    <td>{report.title}</td>
                    <td>{formatScanTypeLabel(report.scanType)}</td>
                    <td>
                      <span className={`risk-pill ${riskTone(report.riskLevel)}`}>
                        {formatRiskLabel(report.riskLevel)}
                      </span>
                    </td>
                    <td>{formatDate(report.createdAt)}</td>
                    <td>{truncateText(report.summary, 90)}</td>
                    <td>
                      <button
                        className="ghost-button"
                        disabled={downloadingId === report.id}
                        onClick={() => onDownload(report.id)}
                        type="button"
                      >
                        {downloadingId === report.id ? "Preparing..." : "Download PDF"}
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

export default ReportsSection;

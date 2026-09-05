import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart as ChartJS,
  Legend,
  LinearScale,
  Tooltip
} from "chart.js";
import { Bar, Doughnut } from "react-chartjs-2";

import {
  describeRiskScore,
  formatDate,
  formatRiskLabel,
  formatScanTypeLabel,
  riskTone
} from "../lib/helpers";

ChartJS.register(ArcElement, BarElement, CategoryScale, LinearScale, Legend, Tooltip);

function OverviewSection({ dashboard, loading, onJumpToSection, recentHighlights }) {
  const scanBreakdown = dashboard?.scanBreakdown || {};
  const riskBreakdown = dashboard?.riskBreakdown || {};
  const recentActivity = dashboard?.recentActivity || [];
  const scanEntries = Object.entries(scanBreakdown).filter(([, value]) => value > 0);
  const riskEntries = Object.entries(riskBreakdown).filter(([, value]) => value > 0);
  const averageRiskScore = Math.round(dashboard?.averageRiskScore ?? 0);
  const fraudAlerts = dashboard?.fraudAlerts ?? 0;

  const summaryCards = [
    {
      label: "Total fraud scans",
      value: dashboard?.totalFraudScans ?? 0,
      helper: "All channels combined"
    },
    {
      label: "Fraud alerts",
      value: fraudAlerts,
      helper: "Medium and high risk results"
    },
    {
      label: "Average risk score",
      value: averageRiskScore,
      helper: "Updated from saved reports"
    }
  ];

  const channelCards = [
    {
      id: "sms",
      label: "Scam SMS",
      description: "Urgency language, phishing terms, and link bait.",
      count: scanBreakdown.SMS || 0
    },
    {
      id: "qr",
      label: "QR Shield",
      description: "Decoded destinations and risky redirect targets.",
      count: scanBreakdown.QR || 0
    },
    {
      id: "url",
      label: "Phishing URL",
      description: "HTTPS checks, suspicious domains, and keyword traps.",
      count: scanBreakdown.URL || 0
    },
    {
      id: "transaction",
      label: "Transaction Risk",
      description: "Behavior shifts across amount, device, location, and time.",
      count: scanBreakdown.TRANSACTION || 0
    }
  ];

  let nextActionTitle = "Start a fresh investigation";
  let nextActionBody = "Run a new scan to expand the fraud history and generate recommendations.";

  if (riskBreakdown.HIGH_RISK) {
    nextActionTitle = "High-risk cases need immediate follow-up";
    nextActionBody = `${riskBreakdown.HIGH_RISK} high-risk report(s) are already saved. Review recent activity and export the most urgent cases.`;
  } else if (fraudAlerts > 0) {
    nextActionTitle = "Medium-risk cases should be triaged";
    nextActionBody = `${fraudAlerts} alert(s) are active. Prioritize the channels with the highest scan volume and confirm whether the activity is legitimate.`;
  } else if (dashboard?.totalFraudScans) {
    nextActionTitle = "Workspace posture looks stable";
    nextActionBody = describeRiskScore(averageRiskScore);
  }

  const riskChartData = {
    labels: riskEntries.map(([label]) => formatRiskLabel(label)),
    datasets: [
      {
        data: riskEntries.map(([, value]) => value),
        backgroundColor: ["#17b26a", "#f59e0b", "#ef4444"],
        borderRadius: 12,
        borderWidth: 0
      }
    ]
  };

  const scanChartData = {
    labels: scanEntries.map(([label]) => formatScanTypeLabel(label)),
    datasets: [
      {
        label: "Scans",
        data: scanEntries.map(([, value]) => value),
        backgroundColor: ["#0f766e", "#0284c7", "#fb7185", "#f59e0b"],
        borderRadius: 12
      }
    ]
  };

  return (
    <section className="section-stack">
      <div className="card-grid summary">
        {summaryCards.map((card) => (
          <article className="metric-card" key={card.label}>
            <span>{card.label}</span>
            <strong>{loading ? "..." : card.value}</strong>
            <small>{card.helper}</small>
          </article>
        ))}
      </div>

      <article className="panel-card spotlight-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Recommended response</span>
            <h3>{nextActionTitle}</h3>
            <p>{nextActionBody}</p>
          </div>
          <div className="metric-banner compact">
            <span>Risk posture</span>
            <strong>{averageRiskScore}</strong>
          </div>
        </div>
      </article>

      <div className="card-grid two-up">
        <article className="panel-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Risk distribution</span>
              <h3>Where fraud pressure is accumulating</h3>
            </div>
          </div>

          <div className="chart-frame">
            {riskEntries.length === 0 ? (
              <div className="empty-state">
                Risk distribution will appear after saved reports start accumulating.
              </div>
            ) : (
              <Doughnut
                data={riskChartData}
                options={{
                  cutout: "68%",
                  plugins: {
                    legend: {
                      position: "bottom"
                    }
                  }
                }}
              />
            )}
          </div>
        </article>

        <article className="panel-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Channel activity</span>
              <h3>Which fraud vectors are being analyzed most</h3>
            </div>
          </div>

          <div className="chart-frame">
            {scanEntries.length === 0 ? (
              <div className="empty-state">
                Channel activity will appear after the first fraud investigation is saved.
              </div>
            ) : (
              <Bar
                data={scanChartData}
                options={{
                  plugins: {
                    legend: {
                      display: false
                    }
                  },
                  scales: {
                    y: {
                      beginAtZero: true,
                      ticks: {
                        precision: 0
                      }
                    }
                  }
                }}
              />
            )}
          </div>
        </article>
      </div>

      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Channel spotlight</span>
            <h3>Choose the next fraud workflow</h3>
          </div>
        </div>

        <div className="highlight-grid">
          {channelCards.map((channel) => (
            <article className="highlight-card" key={channel.id}>
              <small>{channel.count} investigation(s) saved</small>
              <strong>{channel.label}</strong>
              <p>{channel.description}</p>
              <button
                className="ghost-button"
                onClick={() => onJumpToSection(channel.id)}
                type="button"
              >
                Open workflow
              </button>
            </article>
          ))}
        </div>
      </article>

      <div className="card-grid two-up">
        <article className="panel-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Guided scans</span>
              <h3>Start the next investigation</h3>
            </div>
          </div>

          <div className="action-grid">
            <button className="action-card" onClick={() => onJumpToSection("sms")} type="button">
              <strong>Analyze suspicious SMS</strong>
              <span>Check urgency language, scam keywords, and embedded links.</span>
            </button>
            <button className="action-card" onClick={() => onJumpToSection("qr")} type="button">
              <strong>Inspect QR destination</strong>
              <span>Decode images and expose risky payment or phishing targets.</span>
            </button>
            <button className="action-card" onClick={() => onJumpToSection("url")} type="button">
              <strong>Check phishing website</strong>
              <span>Review HTTPS, domain patterns, and suspicious URL language.</span>
            </button>
            <button
              className="action-card"
              onClick={() => onJumpToSection("transaction")}
              type="button"
            >
              <strong>Score a transaction</strong>
              <span>Detect high-value anomalies, new devices, and risky behavior shifts.</span>
            </button>
          </div>
        </article>

        <article className="panel-card">
          <div className="panel-heading">
            <div>
              <span className="eyebrow">Recent activity</span>
              <h3>Latest saved fraud reports</h3>
            </div>
          </div>

          <div className="activity-list">
            {recentActivity.length === 0 && (
              <div className="empty-state">No saved reports yet. Run a scan to populate activity.</div>
            )}

            {recentActivity.map((activity) => (
              <article className="activity-item" key={activity.reportId}>
                <div>
                  <strong>{activity.title}</strong>
                  <small>
                    {formatScanTypeLabel(activity.scanType)} | {formatDate(activity.createdAt)}
                  </small>
                </div>
                <span className={`risk-pill ${riskTone(activity.riskLevel)}`}>
                  {formatRiskLabel(activity.riskLevel)}
                </span>
              </article>
            ))}
          </div>
        </article>
      </div>

      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Latest scan intelligence</span>
            <h3>What PaySentry most recently surfaced</h3>
          </div>
        </div>

        <div className="highlight-grid">
          {recentHighlights.length === 0 && (
            <div className="empty-state">
              New scan insights will appear here after you analyze an SMS, URL, QR code, or
              transaction.
            </div>
          )}

          {recentHighlights.map((result) => (
            <article
              className="highlight-card"
              key={`${result.channel || "scan"}-${result.scanId || result.createdAt}`}
            >
              <span className={`risk-pill ${riskTone(result.riskLevel)}`}>
                {formatRiskLabel(result.riskLevel)}
              </span>
              <strong>{formatScanTypeLabel(result.channel)} investigation</strong>
              <p>{result.explanation}</p>
              <small>Risk score: {result.riskScore}</small>
              <small>{formatDate(result.createdAt)}</small>
            </article>
          ))}
        </div>
      </article>
    </section>
  );
}

export default OverviewSection;

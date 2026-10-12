import { useEffect, useState } from "react";

import { api, extractErrorMessage } from "../api/client";
import { formatDate, riskTone } from "../lib/helpers";

function MonitoringSection({
  alerts,
  dashboard,
  loading,
  onAcknowledge,
  onRefresh,
  realtimeStatus
}) {
  const [gmailStatus, setGmailStatus] = useState(null);
  const [gmailError, setGmailError] = useState("");
  const [gmailBusy, setGmailBusy] = useState(false);
  const [acknowledgingId, setAcknowledgingId] = useState(null);
  const [alertError, setAlertError] = useState("");

  async function refreshGmailStatus() {
    setGmailError("");
    try {
      const response = await api.get("/gmail/status");
      setGmailStatus(response.data);
    } catch (error) {
      setGmailError(extractErrorMessage(error));
    }
  }

  useEffect(() => {
    refreshGmailStatus();
  }, []);

  async function connectGmail() {
    setGmailBusy(true);
    setGmailError("");
    try {
      const response = await api.post("/gmail/connect");
      const authorizationUrl = new URL(response.data.authorizationUrl);
      if (authorizationUrl.protocol !== "https:" || authorizationUrl.hostname !== "accounts.google.com") {
        throw new Error("The backend returned an unexpected Google authorization URL.");
      }
      window.location.assign(authorizationUrl.toString());
    } catch (error) {
      setGmailError(extractErrorMessage(error));
      setGmailBusy(false);
    }
  }

  async function syncGmail() {
    setGmailBusy(true);
    setGmailError("");
    try {
      await api.post("/gmail/sync");
      await refreshGmailStatus();
      await onRefresh();
    } catch (error) {
      setGmailError(extractErrorMessage(error));
    } finally {
      setGmailBusy(false);
    }
  }

  async function disconnectGmail() {
    setGmailBusy(true);
    setGmailError("");
    try {
      await api.delete("/gmail/connection");
      await refreshGmailStatus();
    } catch (error) {
      setGmailError(extractErrorMessage(error));
    } finally {
      setGmailBusy(false);
    }
  }

  async function acknowledgeAlert(alert) {
    setAcknowledgingId(alert.id);
    setAlertError("");
    try {
      await onAcknowledge(alert.id);
    } catch (error) {
      setAlertError(extractErrorMessage(error));
    } finally {
      setAcknowledgingId(null);
    }
  }

  return (
    <section className="section-stack">
      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Shared risk engine</span>
            <h3>Account security and simulated bank activity</h3>
            <p>
              This account activity is synthetic and comes from the PaySentry mock-bank service;
              it is not a real bank feed.
            </p>
          </div>
          <div className="monitoring-status">
            <span className={`connection-dot ${realtimeStatus === "Connected" ? "connected" : ""}`} />
            Live alerts: {realtimeStatus}
          </div>
        </div>

        {loading && !dashboard ? (
          <div className="empty-state">Loading your account and alert history…</div>
        ) : !dashboard ? (
          <div className="empty-state">
            Account monitoring is unavailable. Check the backend connection and try refreshing.
          </div>
        ) : (
          <>
            <div className="card-grid summary">
              <article className="metric-card">
                <span>Account</span>
                <strong className="metric-text">{dashboard.accountId}</strong>
                <small>Mock-bank account identifier</small>
              </article>
              <article className="metric-card">
                <span>Available balance</span>
                <strong className="metric-text">
                  {dashboard.currency} {Number(dashboard.availableBalance ?? 0).toLocaleString()}
                </strong>
                <small>Synthetic simulator value</small>
              </article>
              <article className="metric-card">
                <span>Monitored lien</span>
                <strong className="metric-text">
                  {dashboard.currency} {Number(dashboard.lienAmount ?? 0).toLocaleString()}
                </strong>
                <small>PaySentry alerts only; it never modifies a lien</small>
              </article>
            </div>

            <div className="panel-heading monitoring-subheading">
              <div>
                <h3>Recent simulated transactions</h3>
                <p>Transactions are scored by the backend and shared with the Flutter dashboard.</p>
              </div>
            </div>
            {(dashboard.recentTransactions ?? []).length === 0 ? (
              <div className="empty-state">No mock-bank transactions have been received yet.</div>
            ) : (
              <div className="activity-list">
                {dashboard.recentTransactions.map((transaction) => (
                  <article className="monitoring-row" key={transaction.transactionId}>
                    <div>
                      <strong>{transaction.receiver || "Simulated transaction"}</strong>
                      <small>
                        {transaction.location || "Unknown location"} · {transaction.device || "Unknown device"}
                      </small>
                      <small>{formatDate(transaction.timestamp)}</small>
                    </div>
                    <div className="monitoring-risk">
                      <strong>
                        {dashboard.currency} {Number(transaction.amount ?? 0).toLocaleString()}
                      </strong>
                      <span className={`risk-pill ${riskTone(transaction.riskLevel)}`}>
                        {transaction.riskLevel} · {transaction.riskScore}/100
                      </span>
                    </div>
                  </article>
                ))}
              </div>
            )}
          </>
        )}
      </article>

      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Same saved alerts as mobile</span>
            <h3>Unified alert history</h3>
            <p>
              Banking, SMS, URL, QR, and Gmail alerts are generated and stored by the backend.
              Acknowledging an alert records review only.
            </p>
          </div>
          <button className="secondary-button" disabled={loading} onClick={onRefresh} type="button">
            {loading ? "Refreshing…" : "Refresh"}
          </button>
        </div>

        {alertError && <div className="notice error"><strong>Could not update alert</strong><span>{alertError}</span></div>}
        {loading && alerts.length === 0 ? (
          <div className="empty-state">Loading alert history…</div>
        ) : alerts.length === 0 ? (
          <div className="empty-state">No unified risk alerts have been saved for this account.</div>
        ) : (
          <div className="activity-list">
            {alerts.map((alert) => (
              <article className="unified-alert" key={alert.id}>
                <div className="unified-alert-heading">
                  <div>
                    <span className={`risk-pill ${riskTone(alert.riskLevel)}`}>
                      {alert.riskLevel} · {alert.riskScore}/100
                    </span>
                    <h4>{alert.title}</h4>
                    <small>
                      {alert.eventType} · {formatDate(alert.createdAt)} · {alert.status}
                    </small>
                  </div>
                  {alert.status !== "ACKNOWLEDGED" && (
                    <button
                      className="ghost-button"
                      disabled={acknowledgingId === alert.id}
                      onClick={() => acknowledgeAlert(alert)}
                      type="button"
                    >
                      {acknowledgingId === alert.id ? "Saving…" : "Acknowledge"}
                    </button>
                  )}
                </div>
                {alert.eventDetails && <p className="event-details">{alert.eventDetails}</p>}
                {(alert.reasons ?? []).length > 0 && (
                  <ul className="evidence-list">
                    {alert.reasons.map((reason, index) => <li key={`${alert.id}-reason-${index}`}>{reason}</li>)}
                  </ul>
                )}
                {alert.recommendedAction && (
                  <p className="recommended-action">
                    <strong>Recommended action:</strong> {alert.recommendedAction}
                  </p>
                )}
              </article>
            ))}
          </div>
        )}
      </article>

      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Consent-based email protection</span>
            <h3>Gmail phishing monitoring</h3>
            <p>
              The same server-side Gmail connection is used by web and Android. PaySentry requests
              read-only access; message bodies are analyzed without being stored.
            </p>
          </div>
        </div>

        {gmailError && <div className="notice error"><strong>Gmail action failed</strong><span>{gmailError}</span></div>}
        {!gmailStatus ? (
          <div className="empty-state">Checking Gmail connection status…</div>
        ) : (
          <div className="gmail-status">
            <div>
              <strong>
                {gmailStatus.connected
                  ? `Connected: ${gmailStatus.mailboxEmail}`
                  : gmailStatus.configured
                    ? "No mailbox connected"
                    : "Gmail is not configured on this server"}
              </strong>
              <small>{gmailStatus.message}</small>
              {gmailStatus.lastSyncAt && <small>Last sync: {formatDate(gmailStatus.lastSyncAt)}</small>}
            </div>
            <div className="panel-actions">
              <button className="ghost-button" disabled={gmailBusy} onClick={refreshGmailStatus} type="button">
                Refresh status
              </button>
              {gmailStatus.connected ? (
                <>
                  <button className="secondary-button" disabled={gmailBusy} onClick={syncGmail} type="button">
                    {gmailBusy ? "Working…" : "Sync now"}
                  </button>
                  <button className="ghost-button" disabled={gmailBusy} onClick={disconnectGmail} type="button">
                    Disconnect
                  </button>
                </>
              ) : gmailStatus.configured ? (
                <button className="primary-button" disabled={gmailBusy} onClick={connectGmail} type="button">
                  {gmailBusy ? "Opening Google…" : "Connect Gmail"}
                </button>
              ) : null}
            </div>
          </div>
        )}
      </article>
    </section>
  );
}

export default MonitoringSection;

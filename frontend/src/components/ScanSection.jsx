import { describeRiskScore, formatDate, formatRiskLabel, riskTone } from "../lib/helpers";

const sectionCopy = {
  sms: {
    title: "Scam SMS detection",
    description:
      "Paste the message exactly as received. PaySentry scores risky urgency patterns, keywords, and links.",
    tips: [
      "Keep the message unchanged so urgency language and scam keywords are preserved.",
      "Include short links or payment requests if they appear in the message.",
      "Use the example if you want to see a full end-to-end demo quickly."
    ],
    sampleLabel: "Load sample SMS"
  },
  qr: {
    title: "QR code fraud detection",
    description:
      "Upload a QR code image to decode the destination and assess payment or phishing risk.",
    tips: [
      "Use a clear screenshot or photo with the code fully visible.",
      "PaySentry will extract any URL or payload before assigning a risk score.",
      "Best results come from PNG or JPEG images with good contrast."
    ]
  },
  url: {
    title: "Phishing website detection",
    description:
      "Check whether a suspicious link looks legitimate before someone clicks through to it.",
    tips: [
      "Paste the exact page URL, including query parameters if they look suspicious.",
      "If the scheme is missing, PaySentry will assume HTTPS for convenience.",
      "Use this before clicking links from SMS, email, or QR codes."
    ],
    sampleLabel: "Load sample URL"
  },
  transaction: {
    title: "Transaction risk analysis",
    description:
      "Score transaction risk using amount, receiver, device, timing, and behavioral location changes.",
    tips: [
      "Record the amount, receiver, location, and device as accurately as possible.",
      "Unknown devices and new locations are weighted as behavioral risk signals.",
      "Use the sample to preview a higher-risk transaction scenario."
    ],
    sampleLabel: "Load sample transaction"
  }
};

function ScanSection({ busy, form, onApplyExample, onChange, onClearResult, onSubmit, result, type }) {
  const copy = sectionCopy[type];
  const findings = result?.findings || [];
  const recommendations = result?.recommendations || [];

  return (
    <section className="section-stack">
      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Guided workflow</span>
            <h3>{copy.title}</h3>
            <p>{copy.description}</p>
          </div>

          <div className="panel-actions">
            {copy.sampleLabel && (
              <button className="ghost-button" onClick={() => onApplyExample(type)} type="button">
                {copy.sampleLabel}
              </button>
            )}
          </div>
        </div>

        <div className="tip-grid">
          {copy.tips.map((tip) => (
            <article className="tip-card" key={tip}>
              <strong>Tip</strong>
              <span>{tip}</span>
            </article>
          ))}
        </div>

        {type === "sms" && (
          <div className="panel-form">
            <label className="field full">
              <span>Suspicious SMS content</span>
              <textarea
                onChange={(event) => onChange("sms", "messageContent", event.target.value)}
                placeholder="Example: Your account is blocked. Verify immediately and click here..."
                rows="7"
                value={form.messageContent}
              />
            </label>
            <button className="primary-button" disabled={busy} onClick={onSubmit} type="button">
              {busy ? "Scanning SMS..." : "Run SMS scan"}
            </button>
          </div>
        )}

        {type === "qr" && (
          <div className="panel-form">
            <label className="upload-card">
              <span>Upload QR image</span>
              <input
                accept="image/*"
                onChange={(event) => onChange("qr", "file", event.target.files?.[0] || null)}
                type="file"
              />
              <strong>{form.file?.name || "Choose a PNG, JPG, or JPEG file"}</strong>
            </label>
            <button
              className="primary-button"
              disabled={busy || !form.file}
              onClick={onSubmit}
              type="button"
            >
              {busy ? "Decoding QR..." : "Analyze QR code"}
            </button>
          </div>
        )}

        {type === "url" && (
          <div className="panel-form">
            <label className="field full">
              <span>Website or landing page URL</span>
              <input
                onChange={(event) => onChange("url", "url", event.target.value)}
                placeholder="https://secure-bank-update.example"
                type="text"
                value={form.url}
              />
            </label>
            <button className="primary-button" disabled={busy} onClick={onSubmit} type="button">
              {busy ? "Checking URL..." : "Scan URL"}
            </button>
          </div>
        )}

        {type === "transaction" && (
          <div className="panel-form transaction-grid">
            <label className="field">
              <span>Amount</span>
              <input
                onChange={(event) => onChange("transaction", "amount", event.target.value)}
                placeholder="50000"
                type="number"
                value={form.amount}
              />
            </label>

            <label className="field">
              <span>Receiver</span>
              <input
                onChange={(event) => onChange("transaction", "receiver", event.target.value)}
                placeholder="Merchant or account holder"
                type="text"
                value={form.receiver}
              />
            </label>

            <label className="field">
              <span>Location</span>
              <input
                onChange={(event) => onChange("transaction", "location", event.target.value)}
                placeholder="Mumbai"
                type="text"
                value={form.location}
              />
            </label>

            <label className="field">
              <span>Device</span>
              <input
                onChange={(event) => onChange("transaction", "device", event.target.value)}
                placeholder="User iPhone 15"
                type="text"
                value={form.device}
              />
            </label>

            <label className="field full">
              <span>Transaction time</span>
              <input
                onChange={(event) => onChange("transaction", "transactionTime", event.target.value)}
                type="datetime-local"
                value={form.transactionTime}
              />
            </label>

            <button className="primary-button" disabled={busy} onClick={onSubmit} type="button">
              {busy ? "Scoring transaction..." : "Analyze transaction"}
            </button>
          </div>
        )}
      </article>

      <article className="panel-card">
        <div className="panel-heading">
          <div>
            <span className="eyebrow">Latest result</span>
            <h3>Investigation outcome</h3>
          </div>
        </div>

        {!result && (
          <div className="empty-state">
            Submit a {type === "qr" ? "QR image" : type} investigation to view findings and
            recommendations here.
          </div>
        )}

        {result && (
          <div className="result-card">
            <div className="result-topline">
              <span className={`risk-pill ${riskTone(result.riskLevel)}`}>
                {formatRiskLabel(result.riskLevel)}
              </span>
              <div className="panel-actions">
                <small>{formatDate(result.createdAt)}</small>
                <button className="ghost-button" onClick={() => onClearResult(type)} type="button">
                  Clear result
                </button>
              </div>
            </div>

            <div className="card-grid summary result-summary">
              <div className="metric-banner">
                <span>Risk score</span>
                <strong>{result.riskScore}</strong>
              </div>
              <div className="metric-banner">
                <span>Signals found</span>
                <strong>{findings.length}</strong>
              </div>
              <div className="metric-banner">
                <span>Recommended actions</span>
                <strong>{recommendations.length}</strong>
              </div>
            </div>

            <div className="metric-banner result-outlook">
              <span>Risk outlook</span>
              <strong>{describeRiskScore(result.riskScore)}</strong>
            </div>

            <p className="result-explanation">{result.explanation}</p>

            {result.extractedUrl && (
              <div className="detail-chip">
                <strong>Decoded URL:</strong>
                <span>{result.extractedUrl}</span>
              </div>
            )}

            {result.extractedContent && (
              <div className="detail-chip">
                <strong>Decoded content:</strong>
                <span>{result.extractedContent}</span>
              </div>
            )}

            <div className="result-columns">
              <div>
                <h4>Findings</h4>
                {findings.length === 0 ? (
                  <div className="empty-state">No additional fraud indicators were returned.</div>
                ) : (
                  <ul className="list-clean">
                    {findings.map((finding) => (
                      <li key={finding}>{finding}</li>
                    ))}
                  </ul>
                )}
              </div>
              <div>
                <h4>Recommendations</h4>
                {recommendations.length === 0 ? (
                  <div className="empty-state">No additional recommendations were generated.</div>
                ) : (
                  <ul className="list-clean">
                    {recommendations.map((item) => (
                      <li key={item}>{item}</li>
                    ))}
                  </ul>
                )}
              </div>
            </div>
          </div>
        )}
      </article>
    </section>
  );
}

export default ScanSection;

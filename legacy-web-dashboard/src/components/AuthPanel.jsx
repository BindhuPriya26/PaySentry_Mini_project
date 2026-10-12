function AuthPanel({
  authForm,
  authMode,
  busyAction,
  notice,
  onAuthModeChange,
  onForgotPassword,
  onLogin,
  onRegister,
  onUpdateField
}) {
  return (
    <div className="auth-shell">
      <section className="auth-hero">
        <span className="eyebrow">Digital banking defense</span>
        <h1>PaySentry unifies fraud detection into one calm, guided workspace.</h1>
        <p>
          Review risky messages, suspicious URLs, QR destinations, and transaction anomalies from
          a single interface designed to help users act quickly and confidently.
        </p>

        <div className="hero-grid">
          <article className="hero-tile">
            <strong>Multi-channel intelligence</strong>
            <span>SMS, QR, phishing links, behavioral transaction scoring, and case reports.</span>
          </article>
          <article className="hero-tile">
            <strong>Instant guidance</strong>
            <span>Every scan returns a risk level, findings, and practical recommendations.</span>
          </article>
          <article className="hero-tile">
            <strong>Admin oversight</strong>
            <span>Built-in user management, system metrics, and historical report exports.</span>
          </article>
        </div>

        <div className="auth-guidance">
          <strong>What users can do here</strong>
          <span>Detect scam SMS, inspect QR destinations, review URLs, and score risky transfers.</span>
        </div>
      </section>

      <section className="auth-card">
        <div className="auth-tabs">
          <button
            className={authMode === "login" ? "tab active" : "tab"}
            onClick={() => onAuthModeChange("login")}
            type="button"
          >
            Login
          </button>
          <button
            className={authMode === "register" ? "tab active" : "tab"}
            onClick={() => onAuthModeChange("register")}
            type="button"
          >
            Register
          </button>
          <button
            className={authMode === "forgot" ? "tab active" : "tab"}
            onClick={() => onAuthModeChange("forgot")}
            type="button"
          >
            Password help
          </button>
        </div>

        {notice && (
          <div className={`notice ${notice.tone}`}>
            <strong>{notice.title}</strong>
            <span>{notice.message}</span>
          </div>
        )}

        {authMode !== "forgot" && (
          <div className="panel-form">
            {authMode === "register" && (
              <label className="field">
                <span>Full name</span>
                <input
                  onChange={(event) => onUpdateField("fullName", event.target.value)}
                  placeholder="Enter your full name"
                  type="text"
                  value={authForm.fullName}
                />
              </label>
            )}

            <label className="field">
              <span>Email</span>
              <input
                onChange={(event) => onUpdateField("email", event.target.value)}
                placeholder="name@example.com"
                type="email"
                value={authForm.email}
              />
            </label>

            <label className="field">
              <span>Password</span>
              <input
                onChange={(event) => onUpdateField("password", event.target.value)}
                placeholder="Enter a strong password"
                type="password"
                value={authForm.password}
              />
            </label>

            <button
              className="primary-button"
              disabled={busyAction === "login" || busyAction === "register"}
              onClick={authMode === "login" ? onLogin : onRegister}
              type="button"
            >
              {authMode === "login"
                ? busyAction === "login"
                  ? "Signing in..."
                  : "Access dashboard"
                : busyAction === "register"
                  ? "Creating account..."
                  : "Create secure account"}
            </button>

          </div>
        )}

        {authMode === "forgot" && (
          <div className="panel-form">
            <label className="field">
              <span>Account email</span>
              <input
                onChange={(event) => onUpdateField("resetEmail", event.target.value)}
                placeholder="name@example.com"
                type="email"
                value={authForm.resetEmail}
              />
            </label>

            <button
              className="secondary-button"
              disabled={busyAction === "forgot"}
              onClick={onForgotPassword}
              type="button"
            >
              {busyAction === "forgot" ? "Checking..." : "Check password reset availability"}
            </button>

            <p className="auth-footnote">
              Password recovery email delivery is not configured on this backend. This check will
              not reveal or generate a reset token.
            </p>
          </div>
        )}
      </section>
    </div>
  );
}

export default AuthPanel;

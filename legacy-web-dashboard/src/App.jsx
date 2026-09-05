import { startTransition, useEffect, useState } from "react";

import { api, extractErrorMessage, setAuthToken } from "./api/client";
import AdminSection from "./components/AdminSection";
import AuthPanel from "./components/AuthPanel";
import DashboardHeader from "./components/DashboardHeader";
import OverviewSection from "./components/OverviewSection";
import ReportsSection from "./components/ReportsSection";
import ScanSection from "./components/ScanSection";
import Sidebar from "./components/Sidebar";
import { formatDate } from "./lib/helpers";

const STORAGE_KEY = "paysentry-session";

const navItems = [
  { id: "overview", label: "Overview" },
  { id: "sms", label: "Scam SMS" },
  { id: "qr", label: "QR Shield" },
  { id: "url", label: "Phishing URL" },
  { id: "transaction", label: "Transactions" },
  { id: "reports", label: "Reports" },
  { id: "admin", label: "Admin" }
];

function createInitialAuthForm() {
  return {
    fullName: "",
    email: "",
    password: "",
    resetEmail: "",
    resetToken: "",
    newPassword: ""
  };
}

function createInitialScanForms() {
  return {
    sms: {
      messageContent: ""
    },
    url: {
      url: ""
    },
    qr: {
      file: null
    },
    transaction: {
      amount: "",
      receiver: "",
      location: "",
      device: "",
      transactionTime: ""
    }
  };
}

function createEmptyScanResults() {
  return {
    sms: null,
    qr: null,
    url: null,
    transaction: null
  };
}

function buildNotice(tone, title, message) {
  return {
    tone,
    title,
    message
  };
}

function toLocalDateTimeInputValue(date = new Date()) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60000);
  return localDate.toISOString().slice(0, 16);
}

function normalizeUrlValue(value) {
  const trimmedValue = value.trim();

  if (!trimmedValue) {
    return "";
  }

  if (/^https?:\/\//i.test(trimmedValue)) {
    return trimmedValue;
  }

  return `https://${trimmedValue}`;
}

function normalizeScanResult(type, responseData) {
  if (!responseData) {
    return null;
  }

  if (type === "transaction") {
    return {
      ...responseData,
      channel: "TRANSACTION",
      findings: responseData.flags || [],
      scanId: responseData.transactionId
    };
  }

  return {
    ...responseData,
    channel: responseData.channel || type.toUpperCase(),
    findings: responseData.findings || []
  };
}

function validateTransactionForm(transactionForm) {
  if (!transactionForm.amount) {
    return "Enter the transaction amount before running analysis.";
  }

  if (!transactionForm.receiver.trim()) {
    return "Enter the receiver or merchant name.";
  }

  if (!transactionForm.location.trim()) {
    return "Enter the transaction location.";
  }

  if (!transactionForm.device.trim()) {
    return "Enter the device used for the transaction.";
  }

  return "";
}

function loadStoredSession() {
  try {
    const storedValue = window.localStorage.getItem(STORAGE_KEY);
    return storedValue ? JSON.parse(storedValue) : null;
  } catch (error) {
    return null;
  }
}

function App() {
  const [session, setSession] = useState(loadStoredSession);
  const [authMode, setAuthMode] = useState("login");
  const [authForm, setAuthForm] = useState(createInitialAuthForm);
  const [activeView, setActiveView] = useState("overview");
  const [forms, setForms] = useState(createInitialScanForms);
  const [dashboard, setDashboard] = useState(null);
  const [reports, setReports] = useState([]);
  const [adminStats, setAdminStats] = useState(null);
  const [adminUsers, setAdminUsers] = useState([]);
  const [adminReports, setAdminReports] = useState([]);
  const [scanResults, setScanResults] = useState(createEmptyScanResults);
  const [busyAction, setBusyAction] = useState("");
  const [loadingWorkspace, setLoadingWorkspace] = useState(false);
  const [notice, setNotice] = useState(null);
  const [downloadId, setDownloadId] = useState(null);
  const [togglingUserId, setTogglingUserId] = useState(null);
  const [lastSyncedAt, setLastSyncedAt] = useState(null);

  const allowedNavItems =
    session?.role === "ADMIN" ? navItems : navItems.filter((item) => item.id !== "admin");

  const latestScanHighlights = Object.values(scanResults)
    .filter(Boolean)
    .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
    .slice(0, 4);

  useEffect(() => {
    if (session?.role !== "ADMIN" && activeView === "admin") {
      startTransition(() => setActiveView("overview"));
    }
  }, [activeView, session]);

  useEffect(() => {
    if (!session?.token) {
      setAuthToken(null);
      window.localStorage.removeItem(STORAGE_KEY);
      return;
    }

    setAuthToken(session.token);
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(session));

    let ignore = false;

    async function hydrateWorkspace() {
      setLoadingWorkspace(true);

      try {
        const [dashboardResponse, reportsResponse] = await Promise.all([
          api.get("/dashboard"),
          api.get("/reports")
        ]);

        if (ignore) {
          return;
        }

        setDashboard(dashboardResponse.data);
        setReports(reportsResponse.data);
        setLastSyncedAt(new Date().toISOString());

        if (session.role === "ADMIN") {
          const [statsResponse, usersResponse, adminReportsResponse] = await Promise.all([
            api.get("/admin/stats"),
            api.get("/admin/users"),
            api.get("/admin/reports")
          ]);

          if (ignore) {
            return;
          }

          setAdminStats(statsResponse.data);
          setAdminUsers(usersResponse.data);
          setAdminReports(adminReportsResponse.data);
          return;
        }

        setAdminStats(null);
        setAdminUsers([]);
        setAdminReports([]);
      } catch (error) {
        if (!ignore) {
          setNotice(buildNotice("error", "Workspace sync failed", extractErrorMessage(error)));
        }
      } finally {
        if (!ignore) {
          setLoadingWorkspace(false);
        }
      }
    }

    hydrateWorkspace();

    return () => {
      ignore = true;
    };
  }, [session]);

  async function refreshWorkspace() {
    if (!session?.token) {
      return;
    }

    setLoadingWorkspace(true);

    try {
      const [dashboardResponse, reportsResponse] = await Promise.all([
        api.get("/dashboard"),
        api.get("/reports")
      ]);

      setDashboard(dashboardResponse.data);
      setReports(reportsResponse.data);
      setLastSyncedAt(new Date().toISOString());

      if (session.role === "ADMIN") {
        const [statsResponse, usersResponse, adminReportsResponse] = await Promise.all([
          api.get("/admin/stats"),
          api.get("/admin/users"),
          api.get("/admin/reports")
        ]);

        setAdminStats(statsResponse.data);
        setAdminUsers(usersResponse.data);
        setAdminReports(adminReportsResponse.data);
        return;
      }

      setAdminStats(null);
      setAdminUsers([]);
      setAdminReports([]);
    } catch (error) {
      setNotice(buildNotice("error", "Refresh failed", extractErrorMessage(error)));
    } finally {
      setLoadingWorkspace(false);
    }
  }

  function updateAuthField(field, value) {
    setAuthForm((current) => ({
      ...current,
      [field]: value
    }));
  }

  function updateScanField(section, field, value) {
    setForms((current) => ({
      ...current,
      [section]: {
        ...current[section],
        [field]: value
      }
    }));
  }

  function resetScanForm(section) {
    const emptyForms = createInitialScanForms();

    setForms((current) => ({
      ...current,
      [section]: emptyForms[section]
    }));
  }

  function applyScanExample(type) {
    if (type === "sms") {
      setForms((current) => ({
        ...current,
        sms: {
          messageContent:
            "URGENT: Your account has been blocked. Verify your OTP immediately and click here to reactivate secure access: http://bank-verify-now.example"
        }
      }));
      return;
    }

    if (type === "url") {
      setForms((current) => ({
        ...current,
        url: {
          url: "secure-bank-login-alert.example/reset-account"
        }
      }));
      return;
    }

    if (type === "transaction") {
      setForms((current) => ({
        ...current,
        transaction: {
          amount: "85000",
          receiver: "New merchant payout",
          location: "Dubai",
          device: "Unknown Android device",
          transactionTime: toLocalDateTimeInputValue()
        }
      }));
    }
  }

  function clearScanResult(type) {
    setScanResults((current) => ({
      ...current,
      [type]: null
    }));
  }

  async function handleLogin() {
    setBusyAction("login");
    setNotice(null);

    try {
      const response = await api.post("/auth/login", {
        email: authForm.email,
        password: authForm.password
      });

      setSession(response.data);
      setAuthForm(createInitialAuthForm());
      startTransition(() => setActiveView("overview"));
      setNotice(buildNotice("success", "Welcome back", `Signed in at ${formatDate(new Date().toISOString())}.`));
    } catch (error) {
      setNotice(buildNotice("error", "Login failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function handleRegister() {
    setBusyAction("register");
    setNotice(null);

    try {
      const response = await api.post("/auth/register", {
        fullName: authForm.fullName,
        email: authForm.email,
        password: authForm.password
      });

      setSession(response.data);
      setAuthForm(createInitialAuthForm());
      startTransition(() => setActiveView("overview"));
      setNotice(buildNotice("success", "Account created", "Your secure PaySentry workspace is ready."));
    } catch (error) {
      setNotice(buildNotice("error", "Registration failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function handleForgotPassword() {
    setBusyAction("forgot");
    setNotice(null);

    try {
      const response = await api.post("/auth/forgot-password", {
        email: authForm.resetEmail
      });

      updateAuthField("resetToken", response.data.resetToken || "");
      setNotice(
        buildNotice(
          "success",
          "Reset token generated",
          `${response.data.message} Token: ${response.data.resetToken}`
        )
      );
    } catch (error) {
      setNotice(buildNotice("error", "Could not generate token", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function handleResetPassword() {
    setBusyAction("reset");
    setNotice(null);

    try {
      await api.post("/auth/reset-password", {
        token: authForm.resetToken,
        newPassword: authForm.newPassword
      });

      setNotice(buildNotice("success", "Password updated", "You can now sign in with your new password."));
      setAuthMode("login");
      setAuthForm(createInitialAuthForm());
    } catch (error) {
      setNotice(buildNotice("error", "Reset failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function submitSmsScan() {
    const messageContent = forms.sms.messageContent.trim();

    if (!messageContent) {
      setNotice(
        buildNotice("error", "SMS content required", "Paste the suspicious SMS before starting analysis.")
      );
      return;
    }

    setBusyAction("sms");
    setNotice(null);

    try {
      const response = await api.post("/scans/sms", { messageContent });
      setScanResults((current) => ({
        ...current,
        sms: normalizeScanResult("sms", response.data)
      }));
      resetScanForm("sms");
      setNotice(
        buildNotice(
          "success",
          "SMS scan complete",
          "The suspicious message analysis has been saved to your reports."
        )
      );
      await refreshWorkspace();
      startTransition(() => setActiveView("overview"));
    } catch (error) {
      setNotice(buildNotice("error", "SMS scan failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function submitUrlScan() {
    const normalizedUrl = normalizeUrlValue(forms.url.url);

    if (!normalizedUrl) {
      setNotice(buildNotice("error", "URL required", "Paste a website address before scanning."));
      return;
    }

    setBusyAction("url");
    setNotice(null);

    try {
      const response = await api.post("/scans/url", { url: normalizedUrl });
      setScanResults((current) => ({
        ...current,
        url: normalizeScanResult("url", response.data)
      }));
      resetScanForm("url");
      setNotice(
        buildNotice(
          "success",
          "URL scan complete",
          "The phishing assessment has been added to your dashboard."
        )
      );
      await refreshWorkspace();
      startTransition(() => setActiveView("overview"));
    } catch (error) {
      setNotice(buildNotice("error", "URL scan failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function submitQrScan() {
    if (!forms.qr.file) {
      setNotice(buildNotice("error", "QR image required", "Choose a QR image before starting the scan."));
      return;
    }

    setBusyAction("qr");
    setNotice(null);

    try {
      const payload = new FormData();
      payload.append("file", forms.qr.file);

      const response = await api.post("/scans/qr", payload, {
        headers: {
          "Content-Type": "multipart/form-data"
        }
      });

      setScanResults((current) => ({
        ...current,
        qr: normalizeScanResult("qr", response.data)
      }));
      resetScanForm("qr");
      setNotice(
        buildNotice("success", "QR scan complete", "The QR destination has been decoded and risk scored.")
      );
      await refreshWorkspace();
      startTransition(() => setActiveView("overview"));
    } catch (error) {
      setNotice(buildNotice("error", "QR scan failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function submitTransactionAnalysis() {
    const validationMessage = validateTransactionForm(forms.transaction);

    if (validationMessage) {
      setNotice(buildNotice("error", "Transaction details required", validationMessage));
      return;
    }

    setBusyAction("transaction");
    setNotice(null);

    try {
      const payload = {
        ...forms.transaction,
        transactionTime: forms.transaction.transactionTime || null
      };

      const response = await api.post("/scans/transaction", payload);
      setScanResults((current) => ({
        ...current,
        transaction: normalizeScanResult("transaction", response.data)
      }));
      resetScanForm("transaction");
      setNotice(
        buildNotice(
          "success",
          "Transaction analyzed",
          "Behavioral risk signals have been added to your case log."
        )
      );
      await refreshWorkspace();
      startTransition(() => setActiveView("overview"));
    } catch (error) {
      setNotice(buildNotice("error", "Transaction analysis failed", extractErrorMessage(error)));
    } finally {
      setBusyAction("");
    }
  }

  async function handleReportDownload(reportId) {
    setDownloadId(reportId);

    try {
      const response = await api.get(`/reports/${reportId}/pdf`, {
        responseType: "blob"
      });

      const blobUrl = window.URL.createObjectURL(new Blob([response.data]));
      const anchor = document.createElement("a");
      anchor.href = blobUrl;
      anchor.download = `paysentry-report-${reportId}.pdf`;
      anchor.click();
      window.URL.revokeObjectURL(blobUrl);
    } catch (error) {
      setNotice(buildNotice("error", "Download failed", extractErrorMessage(error)));
    } finally {
      setDownloadId(null);
    }
  }

  async function handleToggleUser(userId, active) {
    setTogglingUserId(userId);

    try {
      await api.patch(`/admin/users/${userId}/status?active=${active}`);
      await refreshWorkspace();
      setNotice(
        buildNotice("success", "User access updated", active ? "User access restored." : "User access revoked.")
      );
    } catch (error) {
      setNotice(buildNotice("error", "Could not update user", extractErrorMessage(error)));
    } finally {
      setTogglingUserId(null);
    }
  }

  function handleLogout() {
    setSession(null);
    setDashboard(null);
    setReports([]);
    setAdminStats(null);
    setAdminUsers([]);
    setAdminReports([]);
    setScanResults(createEmptyScanResults());
    setForms(createInitialScanForms());
    setLastSyncedAt(null);
    setActiveView("overview");
    setNotice(buildNotice("success", "Signed out", "Your workspace session has been cleared from this browser."));
  }

  function useDemoCredentials() {
    setAuthMode("login");
    setAuthForm({
      ...createInitialAuthForm(),
      email: "admin@paysentry.local",
      password: "Admin@123"
    });
    setNotice(buildNotice("success", "Demo credentials loaded", "You can sign in with the prefilled admin account."));
  }

  if (!session?.token) {
    return (
      <AuthPanel
        authForm={authForm}
        authMode={authMode}
        busyAction={busyAction}
        notice={notice}
        onAuthModeChange={setAuthMode}
        onForgotPassword={handleForgotPassword}
        onLogin={handleLogin}
        onRegister={handleRegister}
        onResetPassword={handleResetPassword}
        onUpdateField={updateAuthField}
        onUseDemoCredentials={useDemoCredentials}
      />
    );
  }

  return (
    <div className="app-shell">
      <Sidebar
        activeView={activeView}
        dashboard={dashboard}
        navItems={allowedNavItems}
        onLogout={handleLogout}
        onSelectView={(view) => startTransition(() => setActiveView(view))}
        user={session}
      />

      <main className="workspace">
        <DashboardHeader
          dashboard={dashboard}
          lastSyncedAt={lastSyncedAt}
          loading={loadingWorkspace}
          notice={notice}
          onRefresh={refreshWorkspace}
          user={session}
        />

        {activeView === "overview" && (
          <OverviewSection
            dashboard={dashboard}
            loading={loadingWorkspace}
            onJumpToSection={(view) => startTransition(() => setActiveView(view))}
            recentHighlights={latestScanHighlights}
          />
        )}

        {activeView === "sms" && (
          <ScanSection
            busy={busyAction === "sms"}
            form={forms.sms}
            onApplyExample={applyScanExample}
            onChange={updateScanField}
            onClearResult={clearScanResult}
            onSubmit={submitSmsScan}
            result={scanResults.sms}
            type="sms"
          />
        )}

        {activeView === "qr" && (
          <ScanSection
            busy={busyAction === "qr"}
            form={forms.qr}
            onApplyExample={applyScanExample}
            onChange={updateScanField}
            onClearResult={clearScanResult}
            onSubmit={submitQrScan}
            result={scanResults.qr}
            type="qr"
          />
        )}

        {activeView === "url" && (
          <ScanSection
            busy={busyAction === "url"}
            form={forms.url}
            onApplyExample={applyScanExample}
            onChange={updateScanField}
            onClearResult={clearScanResult}
            onSubmit={submitUrlScan}
            result={scanResults.url}
            type="url"
          />
        )}

        {activeView === "transaction" && (
          <ScanSection
            busy={busyAction === "transaction"}
            form={forms.transaction}
            onApplyExample={applyScanExample}
            onChange={updateScanField}
            onClearResult={clearScanResult}
            onSubmit={submitTransactionAnalysis}
            result={scanResults.transaction}
            type="transaction"
          />
        )}

        {activeView === "reports" && (
          <ReportsSection
            downloadingId={downloadId}
            reports={reports}
            onDownload={handleReportDownload}
          />
        )}

        {activeView === "admin" && session.role === "ADMIN" && (
          <AdminSection
            adminReports={adminReports}
            stats={adminStats}
            togglingUserId={togglingUserId}
            users={adminUsers}
            onDownload={handleReportDownload}
            onToggleUser={handleToggleUser}
          />
        )}
      </main>
    </div>
  );
}

export default App;

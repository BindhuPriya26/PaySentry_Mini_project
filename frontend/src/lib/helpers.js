export function formatRiskLabel(value) {
  if (!value) {
    return "Unknown";
  }

  return value
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function riskTone(value) {
  if (value === "HIGH_RISK") {
    return "high";
  }
  if (value === "MEDIUM_RISK") {
    return "medium";
  }
  return "safe";
}

export function formatDate(value) {
  if (!value) {
    return "Not available";
  }

  return new Date(value).toLocaleString("en-IN", {
    dateStyle: "medium",
    timeStyle: "short"
  });
}

export function formatCurrency(value) {
  const amount = Number(value || 0);
  return new Intl.NumberFormat("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2
  }).format(amount);
}

export function truncateText(value, maxLength = 120) {
  if (!value || value.length <= maxLength) {
    return value || "";
  }

  return `${value.slice(0, maxLength)}...`;
}

export function formatScanTypeLabel(value) {
  if (!value) {
    return "Scan";
  }

  return value
    .toLowerCase()
    .split("_")
    .map((segment) => segment.charAt(0).toUpperCase() + segment.slice(1))
    .join(" ");
}

export function firstName(fullName) {
  if (!fullName) {
    return "Analyst";
  }

  return fullName.trim().split(/\s+/)[0];
}

export function describeRiskScore(score) {
  if (score >= 75) {
    return "Immediate review recommended";
  }

  if (score >= 45) {
    return "Monitor closely";
  }

  return "No urgent signals detected";
}

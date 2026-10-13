import axios from "axios";

function resolveBaseUrl() {
  const configuredUrl = import.meta.env.VITE_API_BASE_URL;
  if (configuredUrl) {
    if (import.meta.env.PROD && !configuredUrl.startsWith("https://")) {
      throw new Error("Production dashboard builds require an HTTPS API URL.");
    }
    return configuredUrl;
  }

  if (import.meta.env.PROD) {
    throw new Error("Production dashboard builds require VITE_API_BASE_URL.");
  }

  if (typeof window === "undefined") {
    return "/api";
  }

  const { hostname, protocol, port } = window.location;
  const runningLocally = hostname === "127.0.0.1" || hostname === "localhost";

  if (runningLocally && port !== "8080") {
    return `${protocol}//${hostname}:8080/api`;
  }

  return "/api";
}

const baseURL = resolveBaseUrl();

export const api = axios.create({
  baseURL
});

let activeRefreshToken = null;
let refreshInProgress = null;
let onSessionUpdated = null;
let onSessionExpired = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const request = error.config;
    const path = request?.url || "";
    const authRequest = path.includes("/auth/login") ||
      path.includes("/auth/register") ||
      path.includes("/auth/refresh") ||
      path.includes("/auth/logout");

    if (error.response?.status !== 401 || authRequest || request?._sessionRefreshAttempted) {
      return Promise.reject(error);
    }

    if (!activeRefreshToken) {
      onSessionExpired?.();
      return Promise.reject(error);
    }

    request._sessionRefreshAttempted = true;
    try {
      refreshInProgress ||= axios.post(`${baseURL}/auth/refresh`, {
        refreshToken: activeRefreshToken
      }).then((response) => {
        const session = response.data;
        activeRefreshToken = session.refreshToken;
        setAuthToken(session.token);
        onSessionUpdated?.(session);
        return session.token;
      }).finally(() => {
        refreshInProgress = null;
      });

      const token = await refreshInProgress;
      request.headers = request.headers || {};
      request.headers.Authorization = `Bearer ${token}`;
      return api(request);
    } catch (refreshError) {
      if (refreshError.response?.status === 401) {
        onSessionExpired?.();
      }
      return Promise.reject(refreshError);
    }
  }
);

export function resolveWebSocketUrl() {
  if (import.meta.env.VITE_WEBSOCKET_URL) {
    if (import.meta.env.PROD && !import.meta.env.VITE_WEBSOCKET_URL.startsWith("wss://")) {
      throw new Error("Production dashboard builds require a secure WSS WebSocket URL.");
    }
    return import.meta.env.VITE_WEBSOCKET_URL;
  }

  if (import.meta.env.PROD) {
    throw new Error("Production dashboard builds require VITE_WEBSOCKET_URL.");
  }

  const apiUrl = new URL(baseURL, window.location.origin);
  const protocol = apiUrl.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${apiUrl.host}/ws`;
}

export function setAuthSession(session) {
  activeRefreshToken = session?.refreshToken || null;
  setAuthToken(session?.token || null);
}

export function setSessionCallbacks({ onUpdated, onExpired }) {
  onSessionUpdated = onUpdated;
  onSessionExpired = onExpired;
}

export function setAuthToken(token) {
  if (token) {
    api.defaults.headers.common.Authorization = `Bearer ${token}`;
    return;
  }

  delete api.defaults.headers.common.Authorization;
}

export function extractErrorMessage(error) {
  return (
    error?.response?.data?.message ||
    error?.response?.data?.error ||
    error?.message ||
    "Something went wrong while talking to the PaySentry backend."
  );
}

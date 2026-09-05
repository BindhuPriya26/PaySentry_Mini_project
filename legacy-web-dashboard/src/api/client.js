import axios from "axios";

function resolveBaseUrl() {
  if (import.meta.env.VITE_API_BASE_URL) {
    return import.meta.env.VITE_API_BASE_URL;
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

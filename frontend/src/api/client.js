import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

// Tokens live in memory first, sessionStorage as a fallback so a refresh
// doesn't force a re-login. Nothing here is ever hardcoded or checked in;
// the backend is the only thing that ever sees real credentials.
let accessToken = sessionStorage.getItem("vm_at") || null;
let refreshToken = sessionStorage.getItem("vm_rt") || null;

export function setTokens(tokens) {
  accessToken = tokens?.accessToken || null;
  refreshToken = tokens?.refreshToken || null;

  if (accessToken) sessionStorage.setItem("vm_at", accessToken);
  else sessionStorage.removeItem("vm_at");

  if (refreshToken) sessionStorage.setItem("vm_rt", refreshToken);
  else sessionStorage.removeItem("vm_rt");
}

export function getAccessToken() {
  return accessToken;
}

export function clearTokens() {
  accessToken = null;
  refreshToken = null;
  sessionStorage.removeItem("vm_at");
  sessionStorage.removeItem("vm_rt");
}

const api = axios.create({ baseURL: BASE_URL });

api.interceptors.request.use((config) => {
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  return config;
});

let refreshPromise = null;

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const original = error.config;

    if (error.response?.status !== 401 || original._retried || !refreshToken) {
      return Promise.reject(error);
    }

    original._retried = true;

    try {
      if (!refreshPromise) {
        refreshPromise = axios
          .post(`${BASE_URL}/auth/refresh`, { refreshToken })
          .then((res) => {
            const data = res.data.data;
            setTokens(data);
            return data.accessToken;
          })
          .finally(() => {
            refreshPromise = null;
          });
      }
      const newAccessToken = await refreshPromise;
      original.headers.Authorization = `Bearer ${newAccessToken}`;
      return api(original);
    } catch (refreshError) {
      clearTokens();
      window.location.href = "/login";
      return Promise.reject(refreshError);
    }
  }
);

export default api;

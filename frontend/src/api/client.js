import axios from "axios";

const BASE_URL = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

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
let sessionExpiredHandled = false;

const sessionListeners = new Set();
export function onSessionExpired(listener) {
    sessionListeners.add(listener);
    return () => sessionListeners.delete(listener);
}

export function handleSessionExpired() {
    clearTokens();
    if (sessionExpiredHandled) return;
    sessionExpiredHandled = true;
    sessionListeners.forEach((l) => l());

    setTimeout(() => {
        sessionExpiredHandled = false;
    }, 2000);
}

export function refreshAccessToken() {
    if (!refreshToken) return Promise.reject(new Error("No refresh token"));
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
    return refreshPromise;
}

api.interceptors.response.use(
    (response) => response,
    async (error) => {
        const original = error.config || {};

        if (!error.response) {
            return Promise.reject(error);
        }

        if (error.response.status !== 401 || original._retried) {
            return Promise.reject(error);
        }

        if (!refreshToken) {
            handleSessionExpired();
            return Promise.reject(error);
        }

        original._retried = true;

        try {
            const newAccessToken = await refreshAccessToken();
            original.headers.Authorization = `Bearer ${newAccessToken}`;
            return api(original);
        } catch (refreshError) {
            handleSessionExpired();
            return Promise.reject(refreshError);
        }
    }
);

export default api;
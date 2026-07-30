import api, { setTokens, clearTokens } from "./client";

export async function register(payload) {
  const res = await api.post("/auth/register", payload);
  setTokens(res.data.data);
  return res.data.data;
}

export async function login(payload) {
  const res = await api.post("/auth/login", payload);
  setTokens(res.data.data);
  return res.data.data;
}

export async function logout(refreshToken) {
  try {
    await api.post("/auth/logout", { refreshToken });
  } finally {
    clearTokens();
  }
}

export function forgotPassword(email) {
  return api.post("/auth/forgot-password", { email });
}

export function resetPassword(payload) {
  return api.post("/auth/reset-password", payload);
}

export function verifyEmail(token) {
  return api.post("/auth/verify-email", { token });
}

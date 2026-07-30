import api from "./client";

// ---- user ----
export const getProfile = () => api.get("/users/me").then((r) => r.data.data);
export const updateProfile = (payload) => api.patch("/users/me", payload).then((r) => r.data.data);
export const changePassword = (payload) => api.post("/users/me/change-password", payload).then((r) => r.data.data);
export const deleteAccount = (payload) => api.delete("/users/me", { data: payload }).then((r) => r.data.data);
export const getMySubscription = () => api.get("/users/me/subscription").then((r) => r.data.data);
export const getMyUsage = () => api.get("/users/me/usage").then((r) => r.data.data);

// ---- organizations ----
export const listOrganizations = () => api.get("/organizations").then((r) => r.data.data);
export const createOrganization = (payload) => api.post("/organizations", payload).then((r) => r.data.data);
export const getOrganization = (orgId) => api.get(`/organizations/${orgId}`).then((r) => r.data.data);
export const listMembers = (orgId) => api.get(`/organizations/${orgId}/members`).then((r) => r.data.data);
export const inviteMember = (orgId, payload) =>
  api.post(`/organizations/${orgId}/members/invite`, payload).then((r) => r.data.data);
export const acceptInvite = (token) =>
  api.post("/organizations/members/accept-invite", { token }).then((r) => r.data.data);
export const updateMemberRole = (orgId, membershipId, role) =>
  api.patch(`/organizations/${orgId}/members/${membershipId}/role`, { role }).then((r) => r.data.data);
export const removeMember = (orgId, membershipId) =>
  api.delete(`/organizations/${orgId}/members/${membershipId}`).then((r) => r.data.data);

// ---- documents ----
export const listDocuments = (orgId, params) =>
  api.get(`/organizations/${orgId}/documents`, { params }).then((r) => r.data.data);
export const searchDocuments = (orgId, q) =>
  api.get(`/organizations/${orgId}/documents/search`, { params: { q } }).then((r) => r.data.data);
export const getDocument = (orgId, docId) =>
  api.get(`/organizations/${orgId}/documents/${docId}`).then((r) => r.data.data);
export const uploadDocument = (orgId, file, onProgress) => {
  const form = new FormData();
  form.append("file", file);
  return api
    .post(`/organizations/${orgId}/documents`, form, {
      headers: { "Content-Type": "multipart/form-data" },
      onUploadProgress: (evt) => onProgress?.(Math.round((evt.loaded * 100) / evt.total)),
    })
    .then((r) => r.data.data);
};
export const deleteDocument = (orgId, docId) =>
  api.delete(`/organizations/${orgId}/documents/${docId}`).then((r) => r.data.data);
export const updateDocument = (orgId, docId, payload) =>
  api.patch(`/organizations/${orgId}/documents/${docId}`, payload).then((r) => r.data.data);
export const addDocumentTag = (orgId, docId, tag) =>
  api.post(`/organizations/${orgId}/documents/${docId}/tags`, { tag }).then((r) => r.data.data);
export const removeDocumentTag = (orgId, docId, tag) =>
  api.delete(`/organizations/${orgId}/documents/${docId}/tags/${tag}`).then((r) => r.data.data);

// ---- folders ----
export const listFolders = (orgId) => api.get(`/organizations/${orgId}/folders`).then((r) => r.data.data);
export const createFolder = (orgId, payload) =>
  api.post(`/organizations/${orgId}/folders`, payload).then((r) => r.data.data);
export const deleteFolder = (orgId, folderId) =>
  api.delete(`/organizations/${orgId}/folders/${folderId}`).then((r) => r.data.data);
export const moveDocument = (orgId, docId, folderId) =>
  api.patch(`/organizations/${orgId}/folders/documents/${docId}/move`, { folderId }).then((r) => r.data.data);

// ---- chat ----
export const listChats = (orgId) => api.get(`/organizations/${orgId}/chats`).then((r) => r.data.data);
export const createChat = (orgId, payload) =>
  api.post(`/organizations/${orgId}/chats`, payload).then((r) => r.data.data);
export const getChat = (orgId, chatId) =>
  api.get(`/organizations/${orgId}/chats/${chatId}`).then((r) => r.data.data);
export const sendMessage = (orgId, chatId, payload) =>
  api.post(`/organizations/${orgId}/chats/${chatId}/messages`, payload).then((r) => r.data.data);
export const renameChat = (orgId, chatId, title) =>
  api.patch(`/organizations/${orgId}/chats/${chatId}`, { title }).then((r) => r.data.data);
export const deleteChat = (orgId, chatId) =>
  api.delete(`/organizations/${orgId}/chats/${chatId}`).then((r) => r.data.data);

// ---- subscriptions & billing ----
export const listPlans = () => api.get("/plans").then((r) => r.data.data);
export const getSubscription = (orgId) =>
  api.get(`/organizations/${orgId}/subscription`).then((r) => r.data.data);
export const upgradeSubscription = (orgId, planId) =>
  api.post(`/organizations/${orgId}/subscription/upgrade`, { planId }).then((r) => r.data.data);
export const cancelSubscription = (orgId) =>
  api.post(`/organizations/${orgId}/subscription/cancel`).then((r) => r.data.data);

export const createPaymentOrder = (orgId, payload) =>
  api.post(`/organizations/${orgId}/payments/create-order`, payload).then((r) => r.data.data);
export const verifyPayment = (orgId, payload) =>
  api.post(`/organizations/${orgId}/payments/verify`, payload).then((r) => r.data.data);
export const paymentHistory = (orgId) =>
  api.get(`/organizations/${orgId}/payments/history`).then((r) => r.data.data);

// ---- usage ----
export const getOrgUsage = (orgId) => api.get(`/organizations/${orgId}/usage`).then((r) => r.data.data);

// ---- ai ----
export const askAi = (orgId, payload) =>
  api.post(`/organizations/${orgId}/ai/ask`, payload).then((r) => r.data.data);

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { OrgProvider } from "./context/OrgContext";
import RequireAuth from "./components/RequireAuth";
import AppShell from "./components/AppShell";

import Login from "./pages/auth/Login";
import Register from "./pages/auth/Register";
import ForgotPassword from "./pages/auth/ForgotPassword";
import ResetPassword from "./pages/auth/ResetPassword";
import VerifyEmail from "./pages/auth/VerifyEmail";
import AcceptInvite from "./pages/org/AcceptInvite";

import Overview from "./pages/Overview";
import Documents from "./pages/documents/Documents";
import ChatList from "./pages/chat/ChatList";
import ChatWindow from "./pages/chat/ChatWindow";
import Organization from "./pages/org/Organization";
import Billing from "./pages/billing/Billing";
import Settings from "./pages/settings/Settings";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <OrgProvider>
          <Routes>
            <Route path="/" element={<Navigate to="/app" replace />} />
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            <Route path="/forgot-password" element={<ForgotPassword />} />
            <Route path="/reset-password" element={<ResetPassword />} />
            <Route path="/verify-email" element={<VerifyEmail />} />
            <Route path="/accept-invite" element={<AcceptInvite />} />

            <Route
              path="/app"
              element={
                <RequireAuth>
                  <AppShell />
                </RequireAuth>
              }
            >
              <Route index element={<Overview />} />
              <Route path="documents" element={<Documents />} />
              <Route path="chats" element={<ChatList />} />
              <Route path="chats/:chatId" element={<ChatWindow />} />
              <Route path="organization" element={<Organization />} />
              <Route path="billing" element={<Billing />} />
              <Route path="settings" element={<Settings />} />
            </Route>

            <Route path="*" element={<Navigate to="/app" replace />} />
          </Routes>
        </OrgProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

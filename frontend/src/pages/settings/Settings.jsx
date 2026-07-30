import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { updateProfile, changePassword, deleteAccount } from "../../api/resources";
import { useToast, getErrorMessage } from "../../context/ToastContext";

export default function Settings() {
  const { user, refreshUser, logout } = useAuth();
  const toast = useToast();
  const navigate = useNavigate();

  const [fullName, setFullName] = useState(user?.fullName || "");
  const [profileMsg, setProfileMsg] = useState("");

  const [pwForm, setPwForm] = useState({ currentPassword: "", newPassword: "" });
  const [pwMsg, setPwMsg] = useState("");

  const [deletePassword, setDeletePassword] = useState("");
  const [deleteError, setDeleteError] = useState("");

  const onSaveProfile = async (e) => {
    e.preventDefault();
    setProfileMsg("");
    try {
      await updateProfile({ fullName });
      await refreshUser();
      setProfileMsg("Profile updated.");
    } catch (err) {
      toast.error(getErrorMessage(err, "Couldn't update your profile."));
    }
  };

  const onChangePassword = async (e) => {
    e.preventDefault();
    setPwMsg("");
    try {
      await changePassword(pwForm);
      setPwForm({ currentPassword: "", newPassword: "" });
      setPwMsg("Password changed.");
    } catch (err) {
      setPwMsg(err.response?.data?.message || "Couldn't change your password.");
    }
  };

  const onDeleteAccount = async (e) => {
    e.preventDefault();
    setDeleteError("");
    if (!confirm("This permanently deletes your account. Continue?")) return;
    try {
      await deleteAccount({ password: deletePassword });
      await logout();
      navigate("/login");
    } catch (err) {
      setDeleteError(err.response?.data?.message || "Couldn't delete your account.");
    }
  };

  return (
    <div>
      <h1>Settings</h1>

      <section className="section">
        <h2>Profile</h2>
        <form className="inline-form" onSubmit={onSaveProfile}>
          <input type="text" value={fullName} onChange={(e) => setFullName(e.target.value)} placeholder="Full name" />
          <button type="submit">Save</button>
        </form>
        {profileMsg && <p className="muted">{profileMsg}</p>}
      </section>

      <section className="section">
        <h2>Change password</h2>
        <form className="inline-form" onSubmit={onChangePassword}>
          <input
            type="password"
            placeholder="Current password"
            value={pwForm.currentPassword}
            onChange={(e) => setPwForm((f) => ({ ...f, currentPassword: e.target.value }))}
            required
          />
          <input
            type="password"
            placeholder="New password"
            value={pwForm.newPassword}
            onChange={(e) => setPwForm((f) => ({ ...f, newPassword: e.target.value }))}
            required
            minLength={8}
          />
          <button type="submit">Update password</button>
        </form>
        {pwMsg && <p className="muted">{pwMsg}</p>}
      </section>

      <section className="section danger-zone">
        <h2>Delete account</h2>
        <p className="muted">This can't be undone. All your documents and chats will be removed.</p>
        <form className="inline-form" onSubmit={onDeleteAccount}>
          <input
            type="password"
            placeholder="Confirm your password"
            value={deletePassword}
            onChange={(e) => setDeletePassword(e.target.value)}
            required
          />
          <button type="submit" className="danger">
            Delete account
          </button>
        </form>
        {deleteError && <div className="alert-error">{deleteError}</div>}
      </section>
    </div>
  );
}

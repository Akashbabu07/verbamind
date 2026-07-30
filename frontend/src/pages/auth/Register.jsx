import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

export default function Register() {
  const { register } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ fullName: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  const onChange = (e) => setForm((f) => ({ ...f, [e.target.name]: e.target.value }));

  const onSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setBusy(true);
    try {
      await register(form);
      navigate("/app", { replace: true });
    } catch (err) {
      setError(err.response?.data?.message || "Something went wrong creating your account.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="auth-page">
      <form className="auth-card" onSubmit={onSubmit}>
        <h1>Create your account</h1>
        <p className="muted">Start exploring documents with AI-assisted search and chat.</p>

        {error && <div className="alert-error">{error}</div>}

        <label>
          Full name
          <input type="text" name="fullName" value={form.fullName} onChange={onChange} required autoFocus />
        </label>

        <label>
          Email
          <input type="email" name="email" value={form.email} onChange={onChange} required />
        </label>

        <label>
          Password
          <input type="password" name="password" value={form.password} onChange={onChange} required minLength={8} />
        </label>

        <button type="submit" disabled={busy}>
          {busy ? "Creating account…" : "Create account"}
        </button>

        <div className="auth-links">
          <Link to="/login">Already have an account? Sign in</Link>
        </div>
      </form>
    </div>
  );
}

import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { acceptInvite } from "../../api/resources";
import { useAuth } from "../../context/AuthContext";
import { useOrg } from "../../context/OrgContext";

export default function AcceptInvite() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const { user, loading } = useAuth();
  const { refresh } = useOrg();
  const [status, setStatus] = useState("pending");

  useEffect(() => {
    if (loading || !user || !token) return;
    acceptInvite(token)
      .then(async () => {
        await refresh();
        setStatus("done");
      })
      .catch(() => setStatus("failed"));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loading, user, token]);

  if (!token) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Invalid invite link</h1>
        </div>
      </div>
    );
  }

  if (!loading && !user) {
    return (
      <div className="auth-page">
        <div className="auth-card">
          <h1>Sign in to accept this invite</h1>
          <Link to={`/login?next=/accept-invite?token=${token}`}>Sign in</Link>
        </div>
      </div>
    );
  }

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Organization invite</h1>
        {status === "pending" && <p className="muted">Joining organization…</p>}
        {status === "done" && (
          <>
            <p className="muted">You've joined the organization.</p>
            <Link to="/app">Go to dashboard</Link>
          </>
        )}
        {status === "failed" && <p className="muted">This invite link is invalid or has expired.</p>}
      </div>
    </div>
  );
}

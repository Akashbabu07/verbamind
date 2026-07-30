import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { verifyEmail } from "../../api/auth";

export default function VerifyEmail() {
  const [params] = useSearchParams();
  const token = params.get("token") || "";
  const [status, setStatus] = useState("verifying");

  useEffect(() => {
    if (!token) {
      setStatus("missing");
      return;
    }
    verifyEmail(token)
      .then(() => setStatus("done"))
      .catch(() => setStatus("failed"));
  }, [token]);

  const copy = {
    verifying: "Verifying your email…",
    done: "Your email is verified. You can sign in now.",
    failed: "This verification link is invalid or has expired.",
    missing: "This link is missing a verification token.",
  };

  return (
    <div className="auth-page">
      <div className="auth-card">
        <h1>Email verification</h1>
        <p className="muted">{copy[status]}</p>
        {(status === "done" || status === "failed" || status === "missing") && (
          <Link to="/login">Go to sign in</Link>
        )}
      </div>
    </div>
  );
}

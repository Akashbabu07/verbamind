import { useEffect, useState } from "react";
import { useOrg } from "../context/OrgContext";
import { getOrgUsage, getSubscription } from "../api/resources";
import { useToast, getErrorMessage } from "../context/ToastContext";

export default function Overview() {
  const { activeOrg, activeOrgId } = useOrg();
  const toast = useToast();
  const [usage, setUsage] = useState(null);
  const [subscription, setSubscription] = useState(null);

  useEffect(() => {
    if (!activeOrgId) return;
    getOrgUsage(activeOrgId)
        .then(setUsage)
        .catch((err) => toast.error(getErrorMessage(err, "Couldn't load usage data.")));
    getSubscription(activeOrgId)
        .then(setSubscription)
        .catch((err) => toast.error(getErrorMessage(err, "Couldn't load your plan.")));

  }, [activeOrgId]);

  if (!activeOrg) {
    return (
        <div className="empty-state">
          <h2>No organization yet</h2>
          <p>Create or join an organization to get started.</p>
        </div>
    );
  }

  return (
      <div>
        <h1>{activeOrg.name}</h1>

        <div className="card-grid">
          <div className="card">
            <h3>Plan</h3>
            <p className="stat">{subscription?.plan?.name || "—"}</p>
            <p className="muted">{subscription?.status || ""}</p>
          </div>
          <div className="card">
            <h3>Documents processed</h3>
            <p className="stat">{usage?.documentsUploaded ?? "—"}</p>
          </div>
          <div className="card">
            <h3>AI queries this period</h3>
            <p className="stat">{usage?.aiRequestsThisMonth ?? "—"}</p>
          </div>
          <div className="card">
            <h3>Storage used</h3>
            <p className="stat">
              {usage?.storageUsedBytes ? `${(usage.storageUsedBytes / (1024 * 1024)).toFixed(1)} MB` : "—"}
            </p>
          </div>
        </div>
      </div>
  );
}
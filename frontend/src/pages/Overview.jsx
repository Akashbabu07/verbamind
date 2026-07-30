import { useEffect, useState } from "react";
import { useOrg } from "../context/OrgContext";
import { getOrgUsage, getSubscription } from "../api/resources";

export default function Overview() {
  const { activeOrg, activeOrgId } = useOrg();
  const [usage, setUsage] = useState(null);
  const [subscription, setSubscription] = useState(null);

  useEffect(() => {
    if (!activeOrgId) return;
    getOrgUsage(activeOrgId).then(setUsage).catch(() => {});
    getSubscription(activeOrgId).then(setSubscription).catch(() => {});
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
          <p className="stat">{subscription?.planName || "—"}</p>
          <p className="muted">{subscription?.status || ""}</p>
        </div>
        <div className="card">
          <h3>Documents processed</h3>
          <p className="stat">{usage?.documentsProcessed ?? "—"}</p>
        </div>
        <div className="card">
          <h3>AI queries this period</h3>
          <p className="stat">{usage?.aiQueries ?? "—"}</p>
        </div>
        <div className="card">
          <h3>Storage used</h3>
          <p className="stat">{usage?.storageUsedMb ? `${usage.storageUsedMb} MB` : "—"}</p>
        </div>
      </div>
    </div>
  );
}

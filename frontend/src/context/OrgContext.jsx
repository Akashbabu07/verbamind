import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { listOrganizations } from "../api/resources";
import { useAuth } from "./AuthContext";

const OrgContext = createContext(null);

export function OrgProvider({ children }) {
  const { user } = useAuth();
  const [organizations, setOrganizations] = useState([]);
  const [activeOrgId, setActiveOrgId] = useState(localStorage.getItem("vm_org") || null);
  const [loading, setLoading] = useState(true);

  const refresh = useCallback(async () => {
    if (!user) {
      setOrganizations([]);
      setLoading(false);
      return;
    }
    setLoading(true);
    try {
      const orgs = await listOrganizations();
      setOrganizations(orgs);

      const stillValid = orgs.some((o) => o.id === activeOrgId);
      if (!stillValid) {
        selectOrg(orgs.length > 0 ? orgs[0].id : null);
      }
    } catch {
      setOrganizations([]);
    } finally {
      setLoading(false);
    }
  }, [user, activeOrgId]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const selectOrg = (orgId) => {
    setActiveOrgId(orgId);
    localStorage.setItem("vm_org", orgId);
  };

  const activeOrg = organizations.find((o) => o.id === activeOrgId) || null;

  return (
    <OrgContext.Provider value={{ organizations, activeOrg, activeOrgId, selectOrg, refresh, loading }}>
      {children}
    </OrgContext.Provider>
  );
}

export function useOrg() {
  const ctx = useContext(OrgContext);
  if (!ctx) throw new Error("useOrg must be used inside OrgProvider");
  return ctx;
}

import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { useOrg } from "../context/OrgContext";

const links = [
  { to: "/app", label: "Overview", end: true },
  { to: "/app/documents", label: "Documents" },
  { to: "/app/chats", label: "Chats" },
  { to: "/app/organization", label: "Organization" },
  { to: "/app/billing", label: "Billing" },
  { to: "/app/settings", label: "Settings" },
];

export default function AppShell() {
  const { user, logout } = useAuth();
  const { organizations, activeOrgId, selectOrg } = useOrg();
  const navigate = useNavigate();

  const handleLogout = async () => {
    try {
      await logout();
    } finally {
      navigate("/login");
    }
  };

  return (
    <div className="shell">
      <aside className="shell-sidebar">
        <div className="brand">Verbamind</div>

        {organizations.length > 0 && (
          <select
            className="org-switcher"
            value={activeOrgId || ""}
            onChange={(e) => selectOrg(e.target.value)}
          >
            {organizations.map((org) => (
              <option key={org.id} value={org.id}>
                {org.name}
              </option>
            ))}
          </select>
        )}

        <nav className="shell-nav">
          {links.map((l) => (
            <NavLink key={l.to} to={l.to} end={l.end} className={({ isActive }) => (isActive ? "active" : "")}>
              {l.label}
            </NavLink>
          ))}
        </nav>

        <div className="shell-footer">
          <div className="me">{user?.email}</div>
          <button className="link-btn" onClick={handleLogout}>
            Sign out
          </button>
        </div>
      </aside>

      <main className="shell-main">
        <Outlet />
      </main>
    </div>
  );
}

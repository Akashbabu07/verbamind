import { useEffect, useState } from "react";
import { useOrg } from "../../context/OrgContext";
import { listMembers, inviteMember, updateMemberRole, removeMember, createOrganization } from "../../api/resources";
import { useToast, getErrorMessage } from "../../context/ToastContext";

export default function Organization() {
  const { activeOrg, activeOrgId, organizations, refresh, selectOrg } = useOrg();
  const toast = useToast();
  const [members, setMembers] = useState([]);
  const [inviteEmail, setInviteEmail] = useState("");
  const [newOrgName, setNewOrgName] = useState("");
  const [busy, setBusy] = useState(false);
  const [notice, setNotice] = useState("");

  useEffect(() => {
    if (!activeOrgId) return;
    listMembers(activeOrgId)
        .then(setMembers)
        .catch((err) => toast.error(getErrorMessage(err, "Couldn't load members.")));
  }, [activeOrgId]);

  const onInvite = async (e) => {
    e.preventDefault();
    setBusy(true);
    setNotice("");
    try {
      await inviteMember(activeOrgId, { email: inviteEmail });
      setNotice(`Invite sent to ${inviteEmail}`);
      setInviteEmail("");
    } catch (err) {
      toast.error(getErrorMessage(err, "Couldn't send the invite."));
    } finally {
      setBusy(false);
    }
  };

  const onRoleChange = async (membershipId, role) => {
    const previous = members;
    setMembers((ms) => ms.map((m) => (m.membershipId === membershipId ? { ...m, role } : m)));
    try {
      await updateMemberRole(activeOrgId, membershipId, role);
    } catch (err) {
      setMembers(previous);
      toast.error(getErrorMessage(err, "Couldn't update the member's role."));
    }
  };

  const onRemove = async (membershipId) => {
    if (!confirm("Remove this member?")) return;
    try {
      await removeMember(activeOrgId, membershipId);
      setMembers((ms) => ms.filter((m) => m.membershipId !== membershipId));
    } catch (err) {
      toast.error(getErrorMessage(err, "Couldn't remove that member."));
    }
  };

  const onCreateOrg = async (e) => {
    e.preventDefault();
    setBusy(true);
    try {
      const org = await createOrganization({ name: newOrgName });
      setNewOrgName("");
      await refresh();
      selectOrg(org.id);
    } catch (err) {
      toast.error(getErrorMessage(err, "Couldn't create the organization."));
    } finally {
      setBusy(false);
    }
  };

  return (
      <div>
        <h1>Organization</h1>

        {organizations.length === 0 ? (
            <form className="inline-form" onSubmit={onCreateOrg}>
              <input
                  type="text"
                  placeholder="Organization name"
                  value={newOrgName}
                  onChange={(e) => setNewOrgName(e.target.value)}
                  required
              />
              <button type="submit" disabled={busy}>
                Create organization
              </button>
            </form>
        ) : (
            <>
              <section className="section">
                <h2>Members — {activeOrg?.name}</h2>
                <table className="data-table">
                  <thead>
                  <tr>
                    <th>Email</th>
                    <th>Role</th>
                    <th></th>
                  </tr>
                  </thead>
                  <tbody>
                  {members.map((m) => (
                      <tr key={m.membershipId}>
                        <td>{m.email}</td>
                        <td>
                          <select value={m.role} onChange={(e) => onRoleChange(m.membershipId, e.target.value)}>
                            <option value="OWNER">Owner</option>
                            <option value="ADMIN">Admin</option>
                            <option value="MEMBER">Member</option>
                          </select>
                        </td>
                        <td>
                          <button className="link-btn danger" onClick={() => onRemove(m.membershipId)}>
                            Remove
                          </button>
                        </td>
                      </tr>
                  ))}
                  </tbody>
                </table>
              </section>

              <section className="section">
                <h2>Invite a member</h2>
                <form className="inline-form" onSubmit={onInvite}>
                  <input
                      type="email"
                      placeholder="teammate@company.com"
                      value={inviteEmail}
                      onChange={(e) => setInviteEmail(e.target.value)}
                      required
                  />
                  <button type="submit" disabled={busy}>
                    Send invite
                  </button>
                </form>
                {notice && <p className="muted">{notice}</p>}
              </section>
            </>
        )}
      </div>
  );
}
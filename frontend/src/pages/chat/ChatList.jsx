import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { useOrg } from "../../context/OrgContext";
import { listChats, createChat, deleteChat } from "../../api/resources";
import { useToast, getErrorMessage } from "../../context/ToastContext";

export default function ChatList() {
  const { activeOrgId } = useOrg();
  const toast = useToast();
  const [chats, setChats] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    if (!activeOrgId) return;
    setLoading(true);
    listChats(activeOrgId)
      .then((data) => setChats(data.items || data))
      .catch((err) => toast.error(getErrorMessage(err, "Couldn't load chats.")))
      .finally(() => setLoading(false));

  }, [activeOrgId]);

  const onNewChat = async () => {
    try {
      const chat = await createChat(activeOrgId, { title: "New chat" });
      navigate(`/app/chats/${chat.id}`);
    } catch (err) {
      toast.error(getErrorMessage(err, "Couldn't start a new chat."));
    }
  };

  const onDelete = async (chatId) => {
    if (!confirm("Delete this chat?")) return;
    try {
      await deleteChat(activeOrgId, chatId);
      setChats((cs) => cs.filter((c) => c.id !== chatId));
    } catch (err) {
      toast.error(getErrorMessage(err, "Couldn't delete that chat."));
    }
  };

  if (!activeOrgId) return <div className="empty-state">Select an organization first.</div>;

  return (
    <div>
      <div className="page-header">
        <h1>Chats</h1>
        <button className="btn primary" onClick={onNewChat}>
          New chat
        </button>
      </div>

      {loading ? (
        <p className="muted">Loading chats…</p>
      ) : chats.length === 0 ? (
        <div className="empty-state">No chats yet. Start one to ask questions about your documents.</div>
      ) : (
        <ul className="list">
          {chats.map((chat) => (
            <li key={chat.id} className="list-row">
              <button className="link-btn" onClick={() => navigate(`/app/chats/${chat.id}`)}>
                {chat.title || "Untitled chat"}
              </button>
              <button className="link-btn danger" onClick={() => onDelete(chat.id)}>
                Delete
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

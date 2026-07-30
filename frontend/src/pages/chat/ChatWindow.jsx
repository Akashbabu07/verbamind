import { useEffect, useRef, useState } from "react";
import { useParams } from "react-router-dom";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { useOrg } from "../../context/OrgContext";
import { getChat } from "../../api/resources";
import { getAccessToken } from "../../api/client";

const API_BASE = import.meta.env.VITE_API_URL || "http://localhost:8080/api";

function parseSseChunk(raw) {
  // Parses one SSE "event" block (lines joined by \n, terminated by a blank line).
  // Format from the backend: "event: token\ndata: hello" or "event: done\ndata: "
  let event = "message";
  const dataLines = [];
  for (const line of raw.split("\n")) {
    if (line.startsWith("event:")) {
      event = line.slice(6).trim();
    } else if (line.startsWith("data:")) {
      // Only strip a single leading space after "data:", per the SSE spec —
      // never trim the rest, or you eat the spaces between streamed words.
      dataLines.push(line.slice(5).replace(/^ /, ""));
    }
  }
  return { event, data: dataLines.join("\n") };
}

export default function ChatWindow() {
  const { chatId } = useParams();
  const { activeOrgId } = useOrg();
  const [messages, setMessages] = useState([]);
  const [input, setInput] = useState("");
  const [streaming, setStreaming] = useState(false);
  const [loadingChat, setLoadingChat] = useState(true);
  const bottomRef = useRef(null);
  const textareaRef = useRef(null);

  useEffect(() => {
    if (!activeOrgId || !chatId) return;
    setLoadingChat(true);
    getChat(activeOrgId, chatId)
      .then((chat) => setMessages(chat.messages || []))
      .finally(() => setLoadingChat(false));
  }, [activeOrgId, chatId]);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth", block: "end" });
  }, [messages]);

  const autoResize = () => {
    const el = textareaRef.current;
    if (!el) return;
    el.style.height = "auto";
    el.style.height = Math.min(el.scrollHeight, 160) + "px";
  };

  const onSend = async (e) => {
    e.preventDefault();
    const content = input.trim();
    if (!content || streaming) return;

    setInput("");
    requestAnimationFrame(autoResize);
    setMessages((m) => [...m, { role: "user", content }, { role: "assistant", content: "" }]);
    setStreaming(true);

    try {
      const response = await fetch(
        `${API_BASE}/organizations/${activeOrgId}/chats/${chatId}/messages/stream`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
            Accept: "text/event-stream",
            Authorization: `Bearer ${getAccessToken()}`,
          },
          body: JSON.stringify({ content }),
        }
      );

      if (!response.ok || !response.body) {
        throw new Error("stream failed");
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // SSE events are separated by a blank line (\n\n).
        const events = buffer.split("\n\n");
        buffer = events.pop(); // last chunk may be incomplete, keep it for next read

        for (const raw of events) {
          if (!raw.trim()) continue;
          const { event, data } = parseSseChunk(raw);

          if (event === "done") continue;
          if (!data) continue;

          setMessages((m) => {
            const next = [...m];
            const last = next[next.length - 1];
            next[next.length - 1] = { ...last, content: last.content + data };
            return next;
          });
        }
      }
    } catch {
      setMessages((m) => {
        const next = [...m];
        next[next.length - 1] = {
          role: "assistant",
          content: "Sorry, something went wrong generating a response. Please try again.",
        };
        return next;
      });
    } finally {
      setStreaming(false);
    }
  };

  const onKeyDown = (e) => {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      onSend(e);
    }
  };

  if (loadingChat) {
    return <div className="chat-loading">Loading conversation…</div>;
  }

  return (
    <div className="chat-window">
      <div className="chat-messages">
        {messages.length === 0 && (
          <div className="chat-empty">
            <div className="chat-empty-badge">✦</div>
            <p>Ask anything about your documents to get started.</p>
          </div>
        )}

        {messages.map((msg, idx) => {
          const isLast = idx === messages.length - 1;
          const isEmptyStreamingAssistant = msg.role === "assistant" && !msg.content && streaming && isLast;
          return (
            <div key={idx} className={`chat-row ${msg.role}`}>
              <div className={`chat-avatar ${msg.role}`}>{msg.role === "user" ? "Y" : "V"}</div>
              <div className={`chat-bubble ${msg.role}`}>
                {isEmptyStreamingAssistant ? (
                  <span className="typing-dots">
                    <span></span>
                    <span></span>
                    <span></span>
                  </span>
                ) : msg.role === "assistant" ? (
                  <div className="markdown-body">
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>{msg.content}</ReactMarkdown>
                  </div>
                ) : (
                  <span className="plain-text">{msg.content}</span>
                )}
              </div>
            </div>
          );
        })}
        <div ref={bottomRef} />
      </div>

      <form className="chat-input" onSubmit={onSend}>
        <textarea
          ref={textareaRef}
          rows={1}
          placeholder="Ask something about your documents… (Enter to send, Shift+Enter for new line)"
          value={input}
          onChange={(e) => {
            setInput(e.target.value);
            autoResize();
          }}
          onKeyDown={onKeyDown}
          disabled={streaming}
        />
        <button type="submit" className="send-btn" disabled={streaming || !input.trim()} aria-label="Send message">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M4 12L20 4L14 20L11 13L4 12Z" stroke="currentColor" strokeWidth="2" strokeLinejoin="round" strokeLinecap="round" fill="none" />
          </svg>
        </button>
      </form>
    </div>
  );
}

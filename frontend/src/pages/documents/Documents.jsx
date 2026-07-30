import { useCallback, useEffect, useRef, useState } from "react";
import { useOrg } from "../../context/OrgContext";
import api from "../../api/client";
import {
  listDocuments,
  uploadDocument,
  deleteDocument,
  searchDocuments,
} from "../../api/resources";

export default function Documents() {
  const { activeOrgId } = useOrg();
  const [documents, setDocuments] = useState([]);
  const [query, setQuery] = useState("");
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [loading, setLoading] = useState(true);
  const fileInputRef = useRef(null);

  const load = useCallback(async () => {
    if (!activeOrgId) return;
    setLoading(true);
    try {
      const data = await listDocuments(activeOrgId);
      setDocuments(data.items || data);
    } finally {
      setLoading(false);
    }
  }, [activeOrgId]);

  useEffect(() => {
    load();
  }, [load]);

  const onSearch = async (e) => {
    e.preventDefault();
    if (!query.trim()) return load();
    const results = await searchDocuments(activeOrgId, query.trim());
    setDocuments(results.items || results);
  };

  const onUpload = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploading(true);
    setProgress(0);
    try {
      await uploadDocument(activeOrgId, file, setProgress);
      await load();
    } finally {
      setUploading(false);
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  };

  const onDelete = async (docId) => {
    if (!confirm("Delete this document?")) return;
    await deleteDocument(activeOrgId, docId);
    setDocuments((docs) => docs.filter((d) => d.id !== docId));
  };

  const onDownload = async (doc) => {
    const res = await api.get(`/organizations/${activeOrgId}/documents/${doc.id}/download`, {
      responseType: "blob",
    });
    const url = URL.createObjectURL(res.data);
    const link = document.createElement("a");
    link.href = url;
    link.download = doc.fileName || doc.name || "document";
    link.click();
    URL.revokeObjectURL(url);
  };

  if (!activeOrgId) return <div className="empty-state">Select an organization first.</div>;

  return (
    <div>
      <div className="page-header">
        <h1>Documents</h1>
        <label className="btn primary">
          {uploading ? `Uploading ${progress}%` : "Upload document"}
          <input ref={fileInputRef} type="file" hidden onChange={onUpload} disabled={uploading} />
        </label>
      </div>

      <form className="search-bar" onSubmit={onSearch}>
        <input
          type="text"
          placeholder="Search documents…"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
        />
        <button type="submit">Search</button>
      </form>

      {loading ? (
        <p className="muted">Loading documents…</p>
      ) : documents.length === 0 ? (
        <div className="empty-state">No documents yet. Upload your first one above.</div>
      ) : (
        <table className="data-table">
          <thead>
            <tr>
              <th>Name</th>
              <th>Status</th>
              <th>Size</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {documents.map((doc) => (
              <tr key={doc.id}>
                <td>{doc.fileName || doc.name}</td>
                <td>
                  <span className={`badge ${doc.status?.toLowerCase()}`}>{doc.status}</span>
                </td>
                <td>{doc.sizeBytes ? `${Math.round(doc.sizeBytes / 1024)} KB` : "—"}</td>
                <td className="row-actions">
                  <button className="link-btn" onClick={() => onDownload(doc)}>
                    Download
                  </button>
                  <button className="link-btn danger" onClick={() => onDelete(doc.id)}>
                    Delete
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

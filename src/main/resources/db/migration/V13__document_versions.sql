CREATE TABLE document_versions (
                                   id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                   document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                   version_number INT NOT NULL,
                                   storage_key VARCHAR(500) NOT NULL,
                                   file_size BIGINT NOT NULL,
                                   content_hash VARCHAR(64),
                                   uploaded_by UUID NOT NULL REFERENCES users(id),
                                   created_at TIMESTAMP NOT NULL DEFAULT now(),
                                   UNIQUE (document_id, version_number)
);

CREATE INDEX idx_document_versions_document_id ON document_versions(document_id);

ALTER TABLE documents ADD COLUMN current_version INT NOT NULL DEFAULT 1;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE TABLE document_chunks (
                                 id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                                 document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                                 organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                                 chunk_index INT NOT NULL,
                                 content TEXT NOT NULL,
                                 embedding vector(768),
                                 created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_chunks_document_id ON document_chunks(document_id);
CREATE INDEX idx_chunks_organization_id ON document_chunks(organization_id);

CREATE INDEX idx_chunks_embedding ON document_chunks
    USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100);
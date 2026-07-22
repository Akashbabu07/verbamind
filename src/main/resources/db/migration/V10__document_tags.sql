CREATE TABLE document_tags (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               document_id UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
                               tag VARCHAR(50) NOT NULL,
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               UNIQUE (document_id, tag)
);

CREATE INDEX idx_document_tags_document_id ON document_tags(document_id);
CREATE INDEX idx_document_tags_tag ON document_tags(tag);
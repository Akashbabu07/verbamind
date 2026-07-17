CREATE TABLE documents (
                           id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                           organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                           owner_id UUID NOT NULL REFERENCES users(id),
                           file_name VARCHAR(255) NOT NULL,
                           original_file_name VARCHAR(255) NOT NULL,
                           content_type VARCHAR(100) NOT NULL,
                           file_size BIGINT NOT NULL,
                           storage_key VARCHAR(512) NOT NULL UNIQUE,
                           status VARCHAR(50) NOT NULL DEFAULT 'UPLOADED',
                           created_at TIMESTAMP NOT NULL DEFAULT now(),
                           updated_at TIMESTAMP NOT NULL DEFAULT now(),
                           deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_documents_organization_id ON documents(organization_id);
CREATE INDEX idx_documents_owner_id ON documents(owner_id);
CREATE INDEX idx_documents_file_name ON documents(file_name);
CREATE INDEX idx_documents_status ON documents(status);
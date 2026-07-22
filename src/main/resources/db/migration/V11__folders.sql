CREATE TABLE folders (
                         id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                         organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                         parent_folder_id UUID REFERENCES folders(id) ON DELETE CASCADE,
                         name VARCHAR(255) NOT NULL,
                         created_at TIMESTAMP NOT NULL DEFAULT now(),
                         updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_folders_organization_id ON folders(organization_id);
CREATE INDEX idx_folders_parent_folder_id ON folders(parent_folder_id);

ALTER TABLE documents ADD COLUMN folder_id UUID REFERENCES folders(id) ON DELETE SET NULL;
CREATE INDEX idx_documents_folder_id ON documents(folder_id);
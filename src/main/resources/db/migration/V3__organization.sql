CREATE TABLE organizations (
                               id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                               name VARCHAR(255) NOT NULL,
                               slug VARCHAR(255) NOT NULL UNIQUE,
                               is_personal BOOLEAN NOT NULL DEFAULT FALSE,
                               owner_id UUID NOT NULL REFERENCES users(id),
                               created_at TIMESTAMP NOT NULL DEFAULT now(),
                               updated_at TIMESTAMP NOT NULL DEFAULT now(),
                               deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE memberships (
                             id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                             organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                             user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                             role VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
                             invited_email VARCHAR(255),
                             status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
                             invite_token VARCHAR(255),
                             created_at TIMESTAMP NOT NULL DEFAULT now(),
                             updated_at TIMESTAMP NOT NULL DEFAULT now(),
                             deleted BOOLEAN NOT NULL DEFAULT FALSE,
                             CONSTRAINT uq_membership_org_user UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_organizations_owner_id ON organizations(owner_id);
CREATE INDEX idx_organizations_slug ON organizations(slug);
CREATE INDEX idx_memberships_organization_id ON memberships(organization_id);
CREATE INDEX idx_memberships_user_id ON memberships(user_id);
CREATE INDEX idx_memberships_invite_token ON memberships(invite_token);


ALTER TABLE memberships ALTER COLUMN user_id DROP NOT NULL;
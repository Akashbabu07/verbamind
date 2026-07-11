CREATE TABLE chats (
                       id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                       organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
                       user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                       title VARCHAR(255) NOT NULL DEFAULT 'New Chat',
                       created_at TIMESTAMP NOT NULL DEFAULT now(),
                       updated_at TIMESTAMP NOT NULL DEFAULT now(),
                       deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE messages (
                          id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
                          chat_id UUID NOT NULL REFERENCES chats(id) ON DELETE CASCADE,
                          role VARCHAR(20) NOT NULL,          -- USER | ASSISTANT
                          content TEXT NOT NULL,
                          citations JSONB,                     -- null for USER messages
                          created_at TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at TIMESTAMP NOT NULL DEFAULT now(),
                          deleted BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE INDEX idx_chats_organization_id ON chats(organization_id);
CREATE INDEX idx_chats_user_id ON chats(user_id);
CREATE INDEX idx_messages_chat_id ON messages(chat_id);
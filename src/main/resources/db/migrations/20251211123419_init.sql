-- migrate:up


-- Create attachments table
CREATE TABLE attachments
(
    id           TEXT PRIMARY KEY,
    filename     TEXT                     NOT NULL,
    content_type TEXT                     NOT NULL,
    byte_size    BIGINT                   NOT NULL,
    key          TEXT                     NOT NULL UNIQUE,
    checksum     TEXT                     NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now()),
    deleted_at   TIMESTAMP WITH TIME ZONE NULL
);

CREATE INDEX idx_attachments_created_at ON attachments (created_at);
CREATE INDEX idx_attachments_deleted_at ON attachments (deleted_at);

-- Create image_variant_sets table
CREATE TABLE image_variant_sets
(
    id                     TEXT PRIMARY KEY,
    original_attachment_id TEXT                     NOT NULL REFERENCES attachments (id) ON DELETE CASCADE,
    kind                   TEXT,
    kind_id                TEXT,
    created_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now()),
    updated_at             TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now())
);

-- Create image_variants table
CREATE TABLE image_variants
(
    id                   TEXT PRIMARY KEY,
    attachment_id        TEXT                     NOT NULL REFERENCES attachments (id) ON DELETE CASCADE,
    image_variant_set_id TEXT                     NOT NULL REFERENCES image_variant_sets (id) ON DELETE CASCADE,
    name                 TEXT                     NOT NULL,
    metadata             JSONB,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now())
);

CREATE UNIQUE INDEX idx_image_variant_set_name_unique ON image_variants (image_variant_set_id, name);

CREATE TABLE users
(
    id                VARCHAR(50) PRIMARY KEY,
    password          VARCHAR(255)             NOT NULL,
    email             VARCHAR(255)             NOT NULL UNIQUE,
    access_token      VARCHAR(255)             NOT NULL,
    email_verified_at TIMESTAMP WITH TIME ZONE,
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now()),
    attachment_id     TEXT                     NULL REFERENCES attachments (id) ON DELETE SET NULL
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_access_token ON users (access_token);

-- Create codes table
CREATE TABLE codes
(
    id         VARCHAR(50) PRIMARY KEY,
    value      VARCHAR(255)             NOT NULL,
    type       VARCHAR(50)              NOT NULL,
    owner_type VARCHAR(50)              NOT NULL,
    owner_id   VARCHAR(50)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc', now())
);

CREATE INDEX idx_codes_owner ON codes (owner_type, owner_id);
CREATE INDEX idx_codes_value ON codes (value);
CREATE INDEX idx_codes_expires_at ON codes (expires_at);

-- Create user_image_picture_variant_sets junction table
CREATE TABLE user_image_picture_variant_sets
(
    user_id              VARCHAR(50) NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    image_variant_set_id VARCHAR(50) NOT NULL REFERENCES image_variant_sets (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, image_variant_set_id)
);

-- migrate:down
DROP TABLE IF EXISTS user_image_picture_variant_sets;
DROP TABLE IF EXISTS image_variants;
DROP TABLE IF EXISTS image_variant_sets;
DROP TABLE IF EXISTS attachments;
DROP TABLE IF EXISTS codes;
DROP TABLE IF EXISTS users;

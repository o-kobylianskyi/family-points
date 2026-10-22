-- Private media v1: PostgreSQL owns normalized bytes and metadata.
CREATE TABLE media_assets (
 id uuid PRIMARY KEY, workspace_id bigint NOT NULL REFERENCES families(id),
 mime_type varchar(30) NOT NULL CHECK (mime_type IN ('image/jpeg','image/png')),
 width integer NOT NULL CHECK (width BETWEEN 1 AND 4096),
 height integer NOT NULL CHECK (height BETWEEN 1 AND 4096),
 byte_size integer NOT NULL CHECK (byte_size BETWEEN 1 AND 5242880),
 sha256 char(64) NOT NULL,
 created_by_member_id bigint NOT NULL REFERENCES family_members(id),
 created_at timestamptz NOT NULL DEFAULT now()
);
CREATE TABLE media_blobs (
 asset_id uuid NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
 variant varchar(20) NOT NULL CHECK (variant IN ('CARD','THUMB')),
 bytes bytea NOT NULL,
 PRIMARY KEY(asset_id, variant)
);
CREATE TABLE media_bindings (
 workspace_id bigint NOT NULL REFERENCES families(id),
 target_type varchar(30) NOT NULL CHECK (target_type IN ('WORKSPACE','MEMBER','MEMBER_GROUP','WORK_NODE','TASK')),
 target_id bigint NOT NULL,
 slot varchar(10) NOT NULL CHECK (slot IN ('COVER','ICON','AVATAR')),
 asset_id uuid REFERENCES media_assets(id),
 catalog_key varchar(80),
 version integer NOT NULL DEFAULT 1,
 updated_at timestamptz NOT NULL DEFAULT now(),
 PRIMARY KEY(workspace_id,target_type,target_id,slot),
 CHECK ((asset_id IS NULL) <> (catalog_key IS NULL))
);
CREATE INDEX idx_media_bindings_asset ON media_bindings(asset_id);
CREATE INDEX idx_media_assets_workspace ON media_assets(workspace_id);

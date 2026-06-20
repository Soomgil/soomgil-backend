-- ============================================================
-- V21: community post_retrips + post_snapshot_* (DBML lines 1433-1556)
-- 기존 V13 (hashtags/posts/post_hashtags/post_media/post_likes/post_comments)에 추가.
-- ============================================================

-- ------------------------------------------------------------
-- community.post_snapshot_days
-- ------------------------------------------------------------
CREATE TABLE community.post_snapshot_days (
    id          uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id     uuid        NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    group_type  varchar(30) NOT NULL,
    day_number  int,
    date        date,
    title       varchar(120),
    sort_order  int         NOT NULL DEFAULT 0
);

CREATE INDEX idx_community_post_snapshot_days_post_id        ON community.post_snapshot_days (post_id);
CREATE INDEX idx_community_post_snapshot_days_post_sort      ON community.post_snapshot_days (post_id, sort_order);

-- ------------------------------------------------------------
-- community.post_snapshot_items
-- ------------------------------------------------------------
CREATE TABLE community.post_snapshot_items (
    id                  uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id             uuid         NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    snapshot_day_id     uuid         NOT NULL REFERENCES community.post_snapshot_days(id) ON DELETE CASCADE,
    sort_order          int          NOT NULL,
    place_provider      varchar(40),
    external_place_id   varchar(120),
    source_status       varchar(30)  NOT NULL DEFAULT 'AVAILABLE',
    place_name          varchar(240) NOT NULL,
    address             text,
    lat                 decimal(10,7),
    lng                 decimal(10,7),
    thumbnail_url       text
);

CREATE INDEX idx_community_post_snapshot_items_post_id           ON community.post_snapshot_items (post_id);
CREATE INDEX idx_community_post_snapshot_items_snapshot_day_id   ON community.post_snapshot_items (snapshot_day_id);
CREATE INDEX idx_community_post_snapshot_items_day_sort          ON community.post_snapshot_items (snapshot_day_id, sort_order);
CREATE INDEX idx_community_post_snapshot_items_provider_external ON community.post_snapshot_items (place_provider, external_place_id);

-- ------------------------------------------------------------
-- community.post_snapshot_routes
-- ------------------------------------------------------------
CREATE TABLE community.post_snapshot_routes (
    id                          uuid         PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id                     uuid         NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    origin_snapshot_item_id     uuid         NOT NULL REFERENCES community.post_snapshot_items(id) ON DELETE CASCADE,
    destination_snapshot_item_id uuid        NOT NULL REFERENCES community.post_snapshot_items(id) ON DELETE CASCADE,
    mode                        varchar(20)  NOT NULL,
    geometry_format             varchar(30)  NOT NULL DEFAULT 'GEOJSON',
    geometry                    jsonb        NOT NULL,
    distance_meters             decimal(12,2),
    duration_seconds            decimal(12,2)
);

CREATE INDEX idx_community_post_snapshot_routes_post_id              ON community.post_snapshot_routes (post_id);
CREATE INDEX idx_community_post_snapshot_routes_origin_item_id       ON community.post_snapshot_routes (origin_snapshot_item_id);
CREATE INDEX idx_community_post_snapshot_routes_destination_item_id  ON community.post_snapshot_routes (destination_snapshot_item_id);
CREATE INDEX idx_community_post_snapshot_routes_origin_dest          ON community.post_snapshot_routes (origin_snapshot_item_id, destination_snapshot_item_id);

-- ------------------------------------------------------------
-- community.post_retrips
-- new_trip_id unique (한 번 재탐색된 trip은 다른 retrip에서 재사용 불가)
-- ------------------------------------------------------------
CREATE TABLE community.post_retrips (
    id                uuid        PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id           uuid        NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    user_id           uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    new_trip_id       uuid        NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE,
    snapshot_version  int         NOT NULL,
    created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_community_post_retrips_new_trip_id ON community.post_retrips (new_trip_id);
CREATE INDEX idx_community_post_retrips_post_id           ON community.post_retrips (post_id);
CREATE INDEX idx_community_post_retrips_user_id           ON community.post_retrips (user_id);
CREATE INDEX idx_community_post_retrips_created_at        ON community.post_retrips (created_at);

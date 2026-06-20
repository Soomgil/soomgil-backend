-- ============================================================
-- Community: posts, hashtags, media, snapshot references
-- ============================================================

CREATE SCHEMA IF NOT EXISTS community;

-- ------------------------------------------------------------
-- community.hashtags: 정규화된 해시태그 마스터
-- ------------------------------------------------------------
CREATE TABLE community.hashtags (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name            varchar(60) NOT NULL,
    normalized_name varchar(60) NOT NULL,
    usage_count     integer NOT NULL DEFAULT 0,
    created_at      timestamptz NOT NULL DEFAULT now(),
    updated_at      timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_community_hashtags_normalized_name
    ON community.hashtags (normalized_name);
CREATE INDEX idx_community_hashtags_name ON community.hashtags (name);
CREATE INDEX idx_community_hashtags_usage_count ON community.hashtags (usage_count);

-- ------------------------------------------------------------
-- community.posts: 게시글 루트. 여행방에서 발행된 immutable snapshot.
-- ------------------------------------------------------------
CREATE TABLE community.posts (
    id                       uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    source_trip_id           uuid NOT NULL,
    source_trip_version      bigint NOT NULL,
    published_by_user_id     uuid NOT NULL REFERENCES auth.users(id),
    visibility               varchar(20) NOT NULL DEFAULT 'PUBLIC',
    title                    varchar(180) NOT NULL,
    summary                  varchar(1000),
    cover_media_file_id      uuid,
    snapshot_version         integer NOT NULL DEFAULT 1,
    -- share token: raw는 클라이언트에만, hash를 DB에 저장
    share_token_hash         text,
    share_token_created_at   timestamptz,
    share_token_rotated_at   timestamptz,
    -- moderation
    moderation_status        varchar(20) NOT NULL DEFAULT 'VISIBLE',
    moderation_reason        varchar(255),
    -- lifecycle
    published_at             timestamptz NOT NULL DEFAULT now(),
    deleted_at               timestamptz,
    deleted_reason           varchar(120),
    created_at               timestamptz NOT NULL DEFAULT now(),
    updated_at               timestamptz NOT NULL DEFAULT now()
);

-- published_by_user_id는 users가 삭제되어도 게시글 이력 보존을 위해 FK로 cascade하지 않는다.
-- 대신 auth.users(id) 존재 검증은 애플리케이션에서 처리한다.

CREATE INDEX idx_community_posts_published_by_user_id
    ON community.posts (published_by_user_id);
CREATE INDEX idx_community_posts_visibility
    ON community.posts (visibility);
CREATE INDEX idx_community_posts_moderation_status
    ON community.posts (moderation_status);
CREATE INDEX idx_community_posts_published_at
    ON community.posts (published_at DESC);
CREATE INDEX idx_community_posts_source_trip_id
    ON community.posts (source_trip_id);
CREATE INDEX idx_community_posts_deleted_at
    ON community.posts (deleted_at);

-- 공개 feed용: 삭제/숨김/UNLISTED 제외 빠른 조회
CREATE INDEX idx_community_posts_public_feed
    ON community.posts (published_at DESC)
    WHERE deleted_at IS NULL
      AND moderation_status = 'VISIBLE'
      AND visibility = 'PUBLIC';

-- ------------------------------------------------------------
-- community.post_hashtags: 게시글 ↔ 해시태그 N:M
-- ------------------------------------------------------------
CREATE TABLE community.post_hashtags (
    post_id     uuid NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    hashtag_id  uuid NOT NULL REFERENCES community.hashtags(id) ON DELETE CASCADE,
    created_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, hashtag_id)
);

CREATE INDEX idx_community_post_hashtags_hashtag_id
    ON community.post_hashtags (hashtag_id);

-- ------------------------------------------------------------
-- community.post_media: 게시글에 첨부된 미디어 순서 보존
-- ------------------------------------------------------------
CREATE TABLE community.post_media (
    id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id         uuid NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    media_file_id   uuid NOT NULL,
    sort_order      integer NOT NULL DEFAULT 0,
    caption         varchar(500),
    created_at      timestamptz NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_community_post_media_post_sort
    ON community.post_media (post_id, sort_order);
CREATE INDEX idx_community_post_media_media_file_id
    ON community.post_media (media_file_id);

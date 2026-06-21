-- ============================================================
-- Community: 좋아요, 댓글
-- ============================================================

-- ------------------------------------------------------------
-- community.post_likes: 게시글 좋아요. (post_id, user_id) 복합 PK로 중복 방지.
-- ------------------------------------------------------------
CREATE TABLE community.post_likes (
    post_id     uuid NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    user_id     uuid NOT NULL REFERENCES auth.users(id),
    created_at  timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (post_id, user_id)
);

CREATE INDEX idx_community_post_likes_user_id
    ON community.post_likes (user_id);

-- ------------------------------------------------------------
-- community.post_comments: 게시글 댓글. parent_comment_id로 대댓글 지원.
-- ------------------------------------------------------------
CREATE TABLE community.post_comments (
    id                  uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    post_id             uuid NOT NULL REFERENCES community.posts(id) ON DELETE CASCADE,
    parent_comment_id   uuid REFERENCES community.post_comments(id) ON DELETE CASCADE,
    author_user_id      uuid NOT NULL REFERENCES auth.users(id),
    content             text NOT NULL,
    depth               integer NOT NULL DEFAULT 0,
    moderation_status   varchar(20) NOT NULL DEFAULT 'VISIBLE',
    deleted_at          timestamptz,
    deleted_reason      varchar(120),
    created_at          timestamptz NOT NULL DEFAULT now(),
    updated_at          timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_community_post_comments_post_id
    ON community.post_comments (post_id, created_at);
CREATE INDEX idx_community_post_comments_author_user_id
    ON community.post_comments (author_user_id);
CREATE INDEX idx_community_post_comments_parent
    ON community.post_comments (parent_comment_id)
    WHERE parent_comment_id IS NOT NULL;

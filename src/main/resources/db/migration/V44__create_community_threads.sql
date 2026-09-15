-- ============================================================
-- Community Threads: Threads/X형 공개 피드
-- ============================================================
-- 기존 community.posts 계열(여행 스냅샷 게시글)은 삭제하지 않고 deprecated 상태로 보존한다.
-- 신규 공개 피드는 community.threads 계열만 사용한다.
-- 좋아요/답글 수는 기존 community.posts 코드와 동일하게 COUNT(*)로 계산하며
-- denormalized counter를 두지 않아 카운터 정합성 보정 job이 필요 없게 한다.

-- ------------------------------------------------------------
-- community.threads: 짧은 텍스트 + 선택적 이미지로 구성된 공개 쓰레드
-- ------------------------------------------------------------
CREATE TABLE community.threads (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    author_user_id       uuid NOT NULL REFERENCES auth.users(id),
    content              text NOT NULL,
    moderation_status    varchar(20) NOT NULL DEFAULT 'VISIBLE',
    moderation_reason    varchar(255),
    moderated_by_user_id uuid REFERENCES auth.users(id),
    moderated_at         timestamptz,
    deleted_at           timestamptz,
    deleted_reason       varchar(120),
    deleted_by_user_id   uuid REFERENCES auth.users(id),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT community_threads_content_length_check
        CHECK (char_length(content) BETWEEN 1 AND 500),
    CONSTRAINT community_threads_moderation_status_check
        CHECK (moderation_status IN ('VISIBLE', 'HIDDEN', 'DELETED'))
);

-- 공개 피드는 최신순 스캔이므로 moderation_status + created_at DESC 복합 인덱스를 둔다.
CREATE INDEX idx_community_threads_feed
    ON community.threads (moderation_status, created_at DESC, id DESC);
CREATE INDEX idx_community_threads_author
    ON community.threads (author_user_id, created_at DESC);
CREATE INDEX idx_community_threads_deleted_at
    ON community.threads (deleted_at)
    WHERE deleted_at IS NOT NULL;

-- ------------------------------------------------------------
-- community.thread_media: 쓰레드 첨부 이미지. media.media_files를 기준으로 참조한다.
-- 소유권/활성 상태 검증은 media 모듈의 application 경계에서 수행한다.
-- ------------------------------------------------------------
CREATE TABLE community.thread_media (
    thread_id     uuid NOT NULL REFERENCES community.threads(id) ON DELETE CASCADE,
    media_file_id uuid NOT NULL,
    sort_order    integer NOT NULL DEFAULT 0,
    created_at    timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (thread_id, media_file_id)
);

CREATE INDEX idx_community_thread_media_order
    ON community.thread_media (thread_id, sort_order);
CREATE INDEX idx_community_thread_media_file
    ON community.thread_media (media_file_id);

-- ------------------------------------------------------------
-- community.thread_replies: 답글. 중첩은 기존 정책과 동일하게 최대 1단계.
-- ------------------------------------------------------------
CREATE TABLE community.thread_replies (
    id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    thread_id            uuid NOT NULL REFERENCES community.threads(id) ON DELETE CASCADE,
    parent_reply_id      uuid REFERENCES community.thread_replies(id) ON DELETE CASCADE,
    author_user_id       uuid NOT NULL REFERENCES auth.users(id),
    content              text NOT NULL,
    depth                integer NOT NULL DEFAULT 0,
    moderation_status    varchar(20) NOT NULL DEFAULT 'VISIBLE',
    moderation_reason    varchar(255),
    moderated_by_user_id uuid REFERENCES auth.users(id),
    moderated_at         timestamptz,
    deleted_at           timestamptz,
    deleted_reason       varchar(120),
    deleted_by_user_id   uuid REFERENCES auth.users(id),
    created_at           timestamptz NOT NULL DEFAULT now(),
    updated_at           timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT community_thread_replies_content_length_check
        CHECK (char_length(content) BETWEEN 1 AND 500),
    CONSTRAINT community_thread_replies_moderation_status_check
        CHECK (moderation_status IN ('VISIBLE', 'HIDDEN', 'DELETED')),
    -- depth 0은 root 답글, depth 1은 답글의 답글. 2단계 이상은 DB에서도 차단한다.
    CONSTRAINT community_thread_replies_depth_check
        CHECK (
            (depth = 0 AND parent_reply_id IS NULL)
            OR (depth = 1 AND parent_reply_id IS NOT NULL)
        )
);

CREATE INDEX idx_community_thread_replies_thread
    ON community.thread_replies (thread_id, created_at, id);
CREATE INDEX idx_community_thread_replies_parent
    ON community.thread_replies (parent_reply_id, created_at)
    WHERE parent_reply_id IS NOT NULL;
CREATE INDEX idx_community_thread_replies_author
    ON community.thread_replies (author_user_id);

-- ------------------------------------------------------------
-- community.thread_likes: 멱등 좋아요.
-- 복합 PK가 "한 사용자당 1개" 제약이자 멱등성의 근거다.
-- ------------------------------------------------------------
CREATE TABLE community.thread_likes (
    thread_id  uuid NOT NULL REFERENCES community.threads(id) ON DELETE CASCADE,
    user_id    uuid NOT NULL REFERENCES auth.users(id),
    created_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (thread_id, user_id)
);

CREATE INDEX idx_community_thread_likes_user
    ON community.thread_likes (user_id);

-- ------------------------------------------------------------
-- 기존 여행 스냅샷 게시글 계열을 deprecated로 표시한다.
-- 데이터와 기존 API는 보존하지만 신규 커뮤니티 피드에서는 사용하지 않는다.
-- ------------------------------------------------------------
COMMENT ON TABLE community.posts IS
    'DEPRECATED: 여행 스냅샷 게시글. 신규 커뮤니티 피드는 community.threads를 사용한다. 데이터 보존용으로만 유지한다.';
COMMENT ON TABLE community.post_comments IS
    'DEPRECATED: 스냅샷 게시글 댓글. 신규 피드는 community.thread_replies를 사용한다.';
COMMENT ON TABLE community.post_likes IS
    'DEPRECATED: 스냅샷 게시글 좋아요. 신규 피드는 community.thread_likes를 사용한다.';
COMMENT ON TABLE community.post_retrips IS
    'DEPRECATED: 리트립. 신규 커뮤니티 피드에서는 사용하지 않는다.';

-- ------------------------------------------------------------
-- 신고/모더레이션 대상 확장.
-- content_reports.target_type과 moderation_actions.target_type은 varchar(20)이고
-- CHECK 제약이 없어 THREAD, THREAD_REPLY 값을 별도 migration 없이 수용한다.
-- ------------------------------------------------------------
COMMENT ON COLUMN community.content_reports.target_type IS
    'POST, POST_COMMENT, THREAD, THREAD_REPLY';
COMMENT ON COLUMN community.moderation_actions.target_type IS
    'POST, POST_COMMENT, THREAD, THREAD_REPLY';

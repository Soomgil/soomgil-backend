package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.domain.model.CommunityThreadReplyRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 커뮤니티 쓰레드 답글 mapper.
 *
 * <p>{@code findById}는 권한 판단을 handler에서 하도록 삭제/숨김 답글도 반환한다.
 * 목록 조회는 root 답글과 1단계 하위 답글을 나눠 가져오고 조립은 assembler가 담당한다.
 */
@Mapper
public interface CommunityThreadReplyMapper {

	/**
	 * 답글을 등록한다.
	 *
	 * @param id 답글 식별자
	 * @param threadId 소속 쓰레드 식별자
	 * @param parentReplyId 부모 답글 식별자. root 답글이면 null
	 * @param authorUserId 작성자
	 * @param content 본문
	 * @param depth 중첩 깊이. 0 또는 1만 허용된다
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.thread_replies
		    (id, thread_id, parent_reply_id, author_user_id, content, depth, created_at, updated_at)
		VALUES
		    (#{id}, #{threadId}, #{parentReplyId}, #{authorUserId}, #{content}, #{depth}, #{now}, #{now})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("threadId") UUID threadId,
		@Param("parentReplyId") UUID parentReplyId,
		@Param("authorUserId") UUID authorUserId,
		@Param("content") String content,
		@Param("depth") int depth,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 답글을 조회한다. 삭제/숨김 답글도 포함한다.
	 *
	 * @param id 답글 식별자
	 * @return 답글. 없으면 empty
	 */
	@Select("""
		SELECT id, thread_id, parent_reply_id, author_user_id, content, depth,
		       moderation_status, deleted_at, created_at, updated_at
		FROM community.thread_replies
		WHERE id = #{id}
		""")
	Optional<CommunityThreadReplyRecord> findById(@Param("id") UUID id);

	/**
	 * 쓰레드의 노출 가능한 root 답글을 오래된 순으로 페이지네이션한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return root 답글 목록
	 */
	@Select("""
		SELECT id, thread_id, parent_reply_id, author_user_id, content, depth,
		       moderation_status, deleted_at, created_at, updated_at
		FROM community.thread_replies
		WHERE thread_id = #{threadId}
		  AND parent_reply_id IS NULL
		  AND deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		ORDER BY created_at, id
		LIMIT #{size} OFFSET #{offset}
		""")
	List<CommunityThreadReplyRecord> findRootReplies(
		@Param("threadId") UUID threadId,
		@Param("offset") int offset,
		@Param("size") int size
	);

	/**
	 * 쓰레드의 노출 가능한 root 답글 수를 센다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @return root 답글 수
	 */
	@Select("""
		SELECT COUNT(*)
		FROM community.thread_replies
		WHERE thread_id = #{threadId}
		  AND parent_reply_id IS NULL
		  AND deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		""")
	long countRootReplies(@Param("threadId") UUID threadId);

	/**
	 * 쓰레드의 노출 가능한 1단계 하위 답글을 모두 조회한다.
	 *
	 * <p>중첩이 1단계로 제한되어 개수가 작기 때문에 부모별 조회 대신 쓰레드 단위로 한 번에 읽는다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @return 1단계 하위 답글 목록
	 */
	@Select("""
		SELECT id, thread_id, parent_reply_id, author_user_id, content, depth,
		       moderation_status, deleted_at, created_at, updated_at
		FROM community.thread_replies
		WHERE thread_id = #{threadId}
		  AND parent_reply_id IS NOT NULL
		  AND deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		ORDER BY created_at, id
		""")
	List<CommunityThreadReplyRecord> findChildReplies(@Param("threadId") UUID threadId);

	/**
	 * 쓰레드의 노출 가능한 전체 답글 수를 센다. root와 하위 답글을 모두 포함한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @return 노출 가능한 답글 수
	 */
	@Select("""
		SELECT COUNT(*)
		FROM community.thread_replies
		WHERE thread_id = #{threadId}
		  AND deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		""")
	int countVisibleByThreadId(@Param("threadId") UUID threadId);

	/**
	 * 답글 본문을 수정한다. 삭제된 답글은 수정되지 않는다.
	 *
	 * @param id 답글 식별자
	 * @param content 새 본문
	 * @param now 수정 시각
	 * @return 수정된 row 수
	 */
	@Update("""
		UPDATE community.thread_replies SET
		    content = #{content},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int updateContent(@Param("id") UUID id, @Param("content") String content, @Param("now") Instant now);

	/**
	 * 답글을 soft delete한다.
	 *
	 * @param id 답글 식별자
	 * @param deletedByUserId 삭제를 수행한 사용자
	 * @param now 삭제 시각
	 * @return 삭제된 row 수. 이미 삭제되어 있으면 0
	 */
	@Update("""
		UPDATE community.thread_replies SET
		    deleted_at = #{now},
		    deleted_by_user_id = #{deletedByUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("id") UUID id,
		@Param("deletedByUserId") UUID deletedByUserId,
		@Param("now") Instant now
	);

	/**
	 * 모더레이션 상태를 갱신한다. 신고 처리 흐름에서만 호출한다.
	 *
	 * @param id 답글 식별자
	 * @param status 새 모더레이션 상태
	 * @param reason 처리 사유
	 * @param moderatorUserId 처리자
	 * @param now 처리 시각
	 */
	@Update("""
		UPDATE community.thread_replies SET
		    moderation_status = #{status},
		    moderation_reason = #{reason},
		    moderated_by_user_id = #{moderatorUserId},
		    moderated_at = #{now},
		    updated_at = #{now}
		WHERE id = #{id}
		""")
	void updateModerationStatus(
		@Param("id") UUID id,
		@Param("status") ModerationStatus status,
		@Param("reason") String reason,
		@Param("moderatorUserId") UUID moderatorUserId,
		@Param("now") Instant now
	);

	/**
	 * 모더레이션 삭제 조치로 답글을 soft delete한다.
	 *
	 * @param id 답글 식별자
	 * @param reason 삭제 사유
	 * @param now 삭제 시각
	 */
	@Update("""
		UPDATE community.thread_replies SET
		    deleted_at = #{now},
		    deleted_reason = #{reason},
		    moderation_status = 'DELETED',
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	void moderationSoftDelete(
		@Param("id") UUID id,
		@Param("reason") String reason,
		@Param("now") Instant now
	);
}

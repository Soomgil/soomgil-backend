package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.domain.model.CommunityCommentRecord;
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
 * 커뮤니티 댓글 mapper.
 *
 * <p>댓글은 soft delete된 것도 포함해 조회({@code findById})하되, 목록 조회({@code findByPostId})에서는
 * 삭제된 댓글을 제외한다.
 */
@Mapper
public interface CommunityCommentMapper {

	/**
	 * 댓글을 등록한다.
	 *
	 * @param id 댓글 식별자
	 * @param postId 게시글 식별자
	 * @param parentCommentId 부모 댓글 (nullable)
	 * @param authorUserId 작성자
	 * @param content 본문
	 * @param depth 중첩 깊이
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.post_comments (id, post_id, parent_comment_id, author_user_id, content, depth, created_at, updated_at)
		VALUES (#{id}, #{postId}, #{parentCommentId}, #{authorUserId}, #{content}, #{depth}, #{now}, #{now})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("postId") UUID postId,
		@Param("parentCommentId") UUID parentCommentId,
		@Param("authorUserId") UUID authorUserId,
		@Param("content") String content,
		@Param("depth") int depth,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 댓글을 조회한다. 삭제된 댓글도 포함.
	 *
	 * @param id 댓글 식별자
	 * @return 댓글. 없으면 empty.
	 */
	@Select("""
		SELECT id, post_id, parent_comment_id, author_user_id, content, depth,
		       moderation_status, deleted_at, created_at
		FROM community.post_comments
		WHERE id = #{id}
		""")
	Optional<CommunityCommentRecord> findById(@Param("id") UUID id);

	/**
	 * 게시글의 활성 댓글을 created_at 오름차순으로 페이지네이션한다.
	 *
	 * <p>삭제된 댓글과 모더레이션 비노출 댓글은 제외한다.
	 *
	 * @param postId 게시글 식별자
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return 댓글 목록
	 */
	@Select("""
		SELECT id, post_id, parent_comment_id, author_user_id, content, depth,
		       moderation_status, deleted_at, created_at
		FROM community.post_comments
		WHERE post_id = #{postId}
		  AND deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		ORDER BY created_at
		LIMIT #{size} OFFSET #{offset}
		""")
	List<CommunityCommentRecord> findByPostId(
		@Param("postId") UUID postId,
		@Param("offset") int offset,
		@Param("size") int size
	);

	/**
	 * 게시글의 활성 댓글 수를 센다.
	 *
	 * @param postId 게시글 식별자
	 * @return 활성 댓글 수
	 */
	@Select("""
		SELECT COUNT(*)
		FROM community.post_comments
		WHERE post_id = #{postId}
		  AND deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		""")
	int countByPostId(@Param("postId") UUID postId);

	/**
	 * 댓글을 soft delete한다.
	 *
	 * @param id 댓글 식별자
	 * @param reason 삭제 사유
	 * @param now 삭제 시각
	 */
	@Update("""
		UPDATE community.post_comments SET
		    deleted_at = #{now},
		    deleted_reason = #{reason},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	void softDelete(@Param("id") UUID id, @Param("reason") String reason, @Param("now") Instant now);

	/**
	 * 댓글의 모더레이션 상태를 갱신한다.
	 *
	 * @param id 댓글 식별자
	 * @param status 새 모더레이션 상태
	 * @param reason 사유
	 * @param now 업데이트 시각
	 */
	@Update("""
		UPDATE community.post_comments SET
		    moderation_status = #{status},
		    updated_at = #{now}
		WHERE id = #{id}
		""")
	void updateModerationStatus(
		@Param("id") UUID id,
		@Param("status") ModerationStatus status,
		@Param("reason") String reason,
		@Param("now") Instant now
	);
}

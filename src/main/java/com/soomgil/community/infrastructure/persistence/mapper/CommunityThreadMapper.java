package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.domain.model.CommunityThreadRecord;
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
 * 커뮤니티 쓰레드 mapper.
 *
 * <p>{@code findById}는 권한/상태 판단을 handler에서 하도록 삭제되거나 숨김 처리된 쓰레드도 반환한다.
 * 반대로 공개 피드 조회인 {@code findFeed}/{@code countFeed}는 삭제와 숨김을 항상 제외한다.
 */
@Mapper
public interface CommunityThreadMapper {

	/**
	 * 쓰레드를 등록한다.
	 *
	 * @param id 쓰레드 식별자
	 * @param authorUserId 작성자
	 * @param content 본문. 길이 검증은 handler와 DB CHECK 제약이 함께 보장한다
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.threads (id, author_user_id, content, created_at, updated_at)
		VALUES (#{id}, #{authorUserId}, #{content}, #{now}, #{now})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("authorUserId") UUID authorUserId,
		@Param("content") String content,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 쓰레드를 조회한다. 삭제/숨김 쓰레드도 포함한다.
	 *
	 * @param id 쓰레드 식별자
	 * @return 쓰레드. 없으면 empty
	 */
	@Select("""
		SELECT id, author_user_id, content, moderation_status, deleted_at, created_at, updated_at
		FROM community.threads
		WHERE id = #{id}
		""")
	Optional<CommunityThreadRecord> findById(@Param("id") UUID id);

	/**
	 * 공개 피드를 최신순으로 조회한다.
	 *
	 * <p>정렬은 {@code created_at DESC, id DESC}로 고정해 같은 시각의 쓰레드에서도 page 경계가 흔들리지 않게 한다.
	 *
	 * @param authorUserId 작성자 필터. 전체 피드면 null
	 * @param query 본문 부분 일치 검색어. 필터를 쓰지 않으면 null
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return 쓰레드 목록
	 */
	@Select("""
		<script>
		SELECT id, author_user_id, content, moderation_status, deleted_at, created_at, updated_at
		FROM community.threads
		WHERE deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		<if test="authorUserId != null">
		  AND author_user_id = #{authorUserId}
		</if>
		<if test="query != null and query != ''">
		  AND content ILIKE ('%' || #{query} || '%')
		</if>
		ORDER BY created_at DESC, id DESC
		LIMIT #{size} OFFSET #{offset}
		</script>
		""")
	List<CommunityThreadRecord> findFeed(
		@Param("authorUserId") UUID authorUserId,
		@Param("query") String query,
		@Param("offset") int offset,
		@Param("size") int size
	);

	/**
	 * 공개 피드의 전체 건수를 센다. 필터 조건은 {@code findFeed}와 동일하다.
	 *
	 * @param authorUserId 작성자 필터. 전체 피드면 null
	 * @param query 본문 부분 일치 검색어. 필터를 쓰지 않으면 null
	 * @return 노출 가능한 쓰레드 수
	 */
	@Select("""
		<script>
		SELECT COUNT(*)
		FROM community.threads
		WHERE deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		<if test="authorUserId != null">
		  AND author_user_id = #{authorUserId}
		</if>
		<if test="query != null and query != ''">
		  AND content ILIKE ('%' || #{query} || '%')
		</if>
		</script>
		""")
	long countFeed(@Param("authorUserId") UUID authorUserId, @Param("query") String query);

	/**
	 * 쓰레드 본문을 수정한다. 삭제된 쓰레드는 수정되지 않는다.
	 *
	 * @param id 쓰레드 식별자
	 * @param content 새 본문
	 * @param now 수정 시각
	 * @return 수정된 row 수
	 */
	@Update("""
		UPDATE community.threads SET
		    content = #{content},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int updateContent(@Param("id") UUID id, @Param("content") String content, @Param("now") Instant now);

	/**
	 * 쓰레드를 soft delete한다.
	 *
	 * @param id 쓰레드 식별자
	 * @param deletedByUserId 삭제를 수행한 사용자
	 * @param now 삭제 시각
	 * @return 삭제된 row 수. 이미 삭제되어 있으면 0
	 */
	@Update("""
		UPDATE community.threads SET
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
	 * @param id 쓰레드 식별자
	 * @param status 새 모더레이션 상태
	 * @param reason 처리 사유
	 * @param moderatorUserId 처리자
	 * @param now 처리 시각
	 */
	@Update("""
		UPDATE community.threads SET
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
	 * 모더레이션 삭제 조치로 쓰레드를 soft delete한다.
	 *
	 * @param id 쓰레드 식별자
	 * @param reason 삭제 사유
	 * @param now 삭제 시각
	 */
	@Update("""
		UPDATE community.threads SET
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

package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.domain.model.CommunityPostRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 커뮤니티 게시글 mapper.
 *
 * <p>INSERT는 {@link CommunityPostMapper#insert} 하나로 모든 칼럼을 받는다.
 * UPDATE는 용도별로 분리: 기본 정보, visibility, share token, moderation, soft delete.
 */
@Mapper
public interface CommunityPostMapper {

	/**
	 * 신규 게시글을 등록한다.
	 *
	 * @param id 게시글 식별자
	 * @param sourceTripId 원본 여행방
	 * @param sourceTripVersion 발행 시점 version
	 * @param publishedByUserId 발행자
	 * @param visibility 공개 범위
	 * @param title 제목
	 * @param summary 요약
	 * @param coverMediaFileId 표지 미디어
	 * @param snapshotVersion snapshot 버전 (1 고정)
	 * @param shareTokenHash 공유 토큰 hash (UNLISTED만)
	 * @param now 발행 시각
	 */
	@org.apache.ibatis.annotations.Insert("""
		INSERT INTO community.posts (
		    id, source_trip_id, source_trip_version, published_by_user_id,
		    visibility, title, summary, cover_media_file_id, snapshot_version,
		    share_token_hash, share_token_created_at, share_token_rotated_at,
		    moderation_status, published_at, created_at, updated_at
		) VALUES (
		    #{id}, #{sourceTripId}, #{sourceTripVersion}, #{publishedByUserId},
		    #{visibility}, #{title}, #{summary}, #{coverMediaFileId}, #{snapshotVersion},
		    #{shareTokenHash}, #{shareTokenCreatedAt}, #{shareTokenRotatedAt},
		    'VISIBLE', #{now}, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("sourceTripId") UUID sourceTripId,
		@Param("sourceTripVersion") long sourceTripVersion,
		@Param("publishedByUserId") UUID publishedByUserId,
		@Param("visibility") PostVisibility visibility,
		@Param("title") String title,
		@Param("summary") String summary,
		@Param("coverMediaFileId") UUID coverMediaFileId,
		@Param("snapshotVersion") int snapshotVersion,
		@Param("shareTokenHash") String shareTokenHash,
		@Param("shareTokenCreatedAt") Instant shareTokenCreatedAt,
		@Param("shareTokenRotatedAt") Instant shareTokenRotatedAt,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 게시글을 조회한다. 삭제된 게시글도 포함.
	 *
	 * @param id 게시글 식별자
	 * @return 게시글. 없으면 empty.
	 */
	@Select("""
		SELECT id, source_trip_id, source_trip_version, published_by_user_id,
		       visibility, title, summary, cover_media_file_id, snapshot_version,
		       share_token_hash, share_token_created_at, share_token_rotated_at,
		       moderation_status, published_at, deleted_at
		FROM community.posts
		WHERE id = #{id}
		""")
	Optional<CommunityPostRecord> findById(@Param("id") UUID id);

	/**
	 * 공개 feed에서 노출 가능한 게시글만 페이지네이션한다.
	 *
	 * <p>조건: deleted_at IS NULL AND moderation_status='VISIBLE' AND visibility='PUBLIC'.
	 * 정렬은 published_at DESC.
	 *
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return 게시글 목록
	 */
	@Select("""
		SELECT id, source_trip_id, source_trip_version, published_by_user_id,
		       visibility, title, summary, cover_media_file_id, snapshot_version,
		       share_token_hash, share_token_created_at, share_token_rotated_at,
		       moderation_status, published_at, deleted_at
		FROM community.posts
		WHERE deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		  AND visibility = 'PUBLIC'
		ORDER BY published_at DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<CommunityPostRecord> findPublicFeed(@Param("offset") int offset, @Param("size") int size);

	/**
	 * 공개 feed 총 게시글 수.
	 *
	 * @return 총 row 수
	 */
	@Select("""
		SELECT COUNT(*)
		FROM community.posts
		WHERE deleted_at IS NULL
		  AND moderation_status = 'VISIBLE'
		  AND visibility = 'PUBLIC'
		""")
	long countPublicFeed();

	/**
	 * 특정 사용자가 발행한 게시글 목록을 페이지네이션한다.
	 *
	 * @param userId 사용자 식별자
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return 게시글 목록
	 */
	@Select("""
		SELECT id, source_trip_id, source_trip_version, published_by_user_id,
		       visibility, title, summary, cover_media_file_id, snapshot_version,
		       share_token_hash, share_token_created_at, share_token_rotated_at,
		       moderation_status, published_at, deleted_at
		FROM community.posts
		WHERE published_by_user_id = #{userId}
		  AND deleted_at IS NULL
		ORDER BY published_at DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<CommunityPostRecord> findByPublisher(
		@Param("userId") UUID userId,
		@Param("offset") int offset,
		@Param("size") int size
	);

	/**
	 * 특정 사용자의 게시글 총 수.
	 *
	 * @param userId 사용자 식별자
	 * @return 총 row 수
	 */
	@Select("""
		SELECT COUNT(*)
		FROM community.posts
		WHERE published_by_user_id = #{userId}
		  AND deleted_at IS NULL
		""")
	long countByPublisher(@Param("userId") UUID userId);

	/**
	 * 게시글의 기본 정보(title/summary/visibility/coverMediaFileId)를 수정한다.
	 *
	 * @param id 게시글 식별자
	 * @param title 제목
	 * @param summary 요약
	 * @param visibility 공개 범위
	 * @param coverMediaFileId 표지 미디어
	 * @param now 업데이트 시각
	 */
	@Update("""
		UPDATE community.posts SET
		    title = #{title},
		    summary = #{summary},
		    visibility = #{visibility},
		    cover_media_file_id = #{coverMediaFileId},
		    updated_at = #{now}
		WHERE id = #{id}
		""")
	void updateBasics(
		@Param("id") UUID id,
		@Param("title") String title,
		@Param("summary") String summary,
		@Param("visibility") PostVisibility visibility,
		@Param("coverMediaFileId") UUID coverMediaFileId,
		@Param("now") Instant now
	);

	/**
	 * 공유 토큰을 새로 설정하거나 rotate한다.
	 *
	 * @param id 게시글 식별자
	 * @param shareTokenHash 새 토큰 hash (raw는 클라이언트에만)
	 * @param now rotate 시각
	 */
	@Update("""
		UPDATE community.posts SET
		    share_token_hash = #{shareTokenHash},
		    share_token_rotated_at = #{now},
		    share_token_created_at = COALESCE(share_token_created_at, #{now}),
		    updated_at = #{now}
		WHERE id = #{id}
		""")
	void updateShareToken(
		@Param("id") UUID id,
		@Param("shareTokenHash") String shareTokenHash,
		@Param("now") Instant now
	);

	/**
	 * soft delete 처리.
	 *
	 * @param id 게시글 식별자
	 * @param reason 삭제 사유
	 * @param now 삭제 시각
	 */
	@Update("""
		UPDATE community.posts SET
		    deleted_at = #{now},
		    deleted_reason = #{reason},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	void softDelete(@Param("id") UUID id, @Param("reason") String reason, @Param("now") Instant now);

	/**
	 * 모더레이션 상태를 갱신한다.
	 *
	 * @param id 게시글 식별자
	 * @param status 새 모더레이션 상태
	 * @param reason 사유
	 * @param now 업데이트 시각
	 */
	@Update("""
		UPDATE community.posts SET
		    moderation_status = #{status},
		    moderation_reason = #{reason},
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

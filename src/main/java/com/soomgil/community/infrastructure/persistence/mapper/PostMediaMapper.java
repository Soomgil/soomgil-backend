package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.domain.model.PostMediaRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 게시글 미디어 mapper.
 *
 * <p>게시글 수정 시 기존 row를 전부 지우고 재삽입한다(post_id + sort_order unique 제약).
 */
@Mapper
public interface PostMediaMapper {

	/**
	 * 미디어 레코드를 삽입한다.
	 *
	 * @param id 미디어 레코드 식별자
	 * @param postId 게시글 식별자
	 * @param mediaFileId 원본 미디어 식별자
	 * @param sortOrder 정렬 순서
	 * @param caption 캡션 (nullable)
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.post_media (id, post_id, media_file_id, sort_order, caption, created_at)
		VALUES (#{id}, #{postId}, #{mediaFileId}, #{sortOrder}, #{caption}, #{now})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("postId") UUID postId,
		@Param("mediaFileId") UUID mediaFileId,
		@Param("sortOrder") int sortOrder,
		@Param("caption") String caption,
		@Param("now") Instant now
	);

	/**
	 * 게시글의 미디어 목록을 sort_order 순으로 조회한다.
	 *
	 * @param postId 게시글 식별자
	 * @return 미디어 레코드 목록
	 */
	@Select("""
		SELECT id, post_id, media_file_id, sort_order, caption, created_at
		FROM community.post_media
		WHERE post_id = #{postId}
		ORDER BY sort_order
		""")
	List<PostMediaRecord> findByPostId(@Param("postId") UUID postId);

	/**
	 * 게시글의 미디어 수를 센다.
	 *
	 * @param postId 게시글 식별자
	 * @return 미디어 개수
	 */
	@Select("SELECT COUNT(*) FROM community.post_media WHERE post_id = #{postId}")
	int countByPostId(@Param("postId") UUID postId);

	/**
	 * 게시글의 모든 미디어를 삭제한다.
	 *
	 * @param postId 게시글 식별자
	 */
	@Delete("DELETE FROM community.post_media WHERE post_id = #{postId}")
	void deleteAllByPostId(@Param("postId") UUID postId);
}

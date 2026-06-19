package com.soomgil.community.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 게시글 좋아요 mapper.
 *
 * <p>{@code (post_id, user_id)} 복합 PK로 중복 좋아요를 방지한다.
 * {@code insertOrIgnore}는 ON CONFLICT DO NOTHING으로 idempotent하게 처리한다.
 */
@Mapper
public interface PostLikeMapper {

	/**
	 * 좋아요를 추가한다. 이미 좋아요한 경우 무시된다.
	 *
	 * @param postId 게시글 식별자
	 * @param userId 사용자 식별자
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.post_likes (post_id, user_id, created_at)
		VALUES (#{postId}, #{userId}, #{now})
		ON CONFLICT (post_id, user_id) DO NOTHING
		""")
	void insertOrIgnore(@Param("postId") UUID postId, @Param("userId") UUID userId, @Param("now") Instant now);

	/**
	 * 좋아요를 취소한다.
	 *
	 * @param postId 게시글 식별자
	 * @param userId 사용자 식별자
	 */
	@Delete("DELETE FROM community.post_likes WHERE post_id = #{postId} AND user_id = #{userId}")
	void delete(@Param("postId") UUID postId, @Param("userId") UUID userId);

	/**
	 * 특정 사용자가 해당 게시글에 좋아요했는지 확인한다.
	 *
	 * @param postId 게시글 식별자
	 * @param userId 사용자 식별자
	 * @return 좋아요 했으면 true
	 */
	@Select("SELECT EXISTS(SELECT 1 FROM community.post_likes WHERE post_id = #{postId} AND user_id = #{userId})")
	boolean existsByPostIdAndUserId(@Param("postId") UUID postId, @Param("userId") UUID userId);

	/**
	 * 게시글의 총 좋아요 수를 센다.
	 *
	 * @param postId 게시글 식별자
	 * @return 좋아요 수
	 */
	@Select("SELECT COUNT(*) FROM community.post_likes WHERE post_id = #{postId}")
	int countByPostId(@Param("postId") UUID postId);
}

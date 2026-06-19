package com.soomgil.community.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 게시글 ↔ 해시태그 연결 테이블 mapper.
 *
 * <p>게시글 수정 시 기존 연결을 모두 지우고 새로 넣는 전략(replace)을 쓴다.
 */
@Mapper
public interface PostHashtagMapper {

	/**
	 * 게시글-해시태그 연결을 추가한다. 중복은 무시된다.
	 *
	 * @param postId 게시글 식별자
	 * @param hashtagId 해시태그 식별자
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.post_hashtags (post_id, hashtag_id, created_at)
		VALUES (#{postId}, #{hashtagId}, #{now})
		ON CONFLICT (post_id, hashtag_id) DO NOTHING
		""")
	void insert(@Param("postId") UUID postId, @Param("hashtagId") UUID hashtagId, @Param("now") Instant now);

	/**
	 * 특정 게시글의 해시태그 이름 목록을 조회한다.
	 *
	 * @param postId 게시글 식별자
	 * @return 해시태그 표시용 이름 목록 (오름차순)
	 */
	@Select("""
		SELECT h.name
		FROM community.post_hashtags ph
		JOIN community.hashtags h ON h.id = ph.hashtag_id
		WHERE ph.post_id = #{postId}
		ORDER BY h.name
		""")
	List<String> findHashtagNamesByPostId(@Param("postId") UUID postId);

	/**
	 * 특정 게시글에 연결된 해시태그 식별자 목록을 조회한다.
	 *
	 * <p>수정 시 기존 연결의 usage_count를 감소시키기 위해 사용한다.
	 *
	 * @param postId 게시글 식별자
	 * @return 해시태그 식별자 목록
	 */
	@Select("SELECT hashtag_id FROM community.post_hashtags WHERE post_id = #{postId}")
	List<UUID> findHashtagIdsByPostId(@Param("postId") UUID postId);

	/**
	 * 특정 게시글의 연결을 모두 삭제한다.
	 *
	 * @param postId 게시글 식별자
	 */
	@Delete("DELETE FROM community.post_hashtags WHERE post_id = #{postId}")
	void deleteAllByPostId(@Param("postId") UUID postId);
}

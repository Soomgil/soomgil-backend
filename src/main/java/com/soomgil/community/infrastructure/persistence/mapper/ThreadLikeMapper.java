package com.soomgil.community.infrastructure.persistence.mapper;

import java.time.Instant;
import java.util.UUID;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 쓰레드 좋아요 mapper.
 *
 * <p>{@code (thread_id, user_id)} 복합 PK가 "사용자당 1개" 제약이며, insert는
 * {@code ON CONFLICT DO NOTHING}으로 멱등하게 동작한다. 좋아요 수는 denormalized counter가 아니라
 * 이 테이블의 COUNT로 계산한다.
 */
@Mapper
public interface ThreadLikeMapper {

	/**
	 * 좋아요를 남긴다. 이미 눌러 둔 상태면 아무것도 변경하지 않는다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param userId 사용자 식별자
	 * @param now 최초 좋아요 시각
	 */
	@Insert("""
		INSERT INTO community.thread_likes (thread_id, user_id, created_at)
		VALUES (#{threadId}, #{userId}, #{now})
		ON CONFLICT (thread_id, user_id) DO NOTHING
		""")
	void insertIfAbsent(
		@Param("threadId") UUID threadId,
		@Param("userId") UUID userId,
		@Param("now") Instant now
	);

	/**
	 * 좋아요를 취소한다. 좋아요가 없어도 실패하지 않는다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param userId 사용자 식별자
	 */
	@Delete("DELETE FROM community.thread_likes WHERE thread_id = #{threadId} AND user_id = #{userId}")
	void delete(@Param("threadId") UUID threadId, @Param("userId") UUID userId);

	/**
	 * 쓰레드의 좋아요 수를 센다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @return 좋아요 수
	 */
	@Select("SELECT COUNT(*) FROM community.thread_likes WHERE thread_id = #{threadId}")
	int countByThreadId(@Param("threadId") UUID threadId);

	/**
	 * 특정 사용자가 좋아요를 눌렀는지 확인한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param userId 사용자 식별자
	 * @return 눌렀으면 true
	 */
	@Select("""
		SELECT EXISTS (
		    SELECT 1 FROM community.thread_likes
		    WHERE thread_id = #{threadId} AND user_id = #{userId}
		)
		""")
	boolean existsByThreadIdAndUserId(@Param("threadId") UUID threadId, @Param("userId") UUID userId);
}

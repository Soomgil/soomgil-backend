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
 * 쓰레드 첨부 이미지 연결 mapper.
 *
 * <p>실제 파일 metadata는 {@code media.media_files}가 기준이고 이 테이블은 순서만 관리한다.
 * 소유권 검증은 handler가 media 모듈의 application 경계를 호출해 수행한다.
 */
@Mapper
public interface ThreadMediaMapper {

	/**
	 * 쓰레드에 미디어를 연결한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @param mediaFileId 미디어 파일 식별자
	 * @param sortOrder 노출 순서. 0부터 시작한다
	 * @param now 연결 시각
	 */
	@Insert("""
		INSERT INTO community.thread_media (thread_id, media_file_id, sort_order, created_at)
		VALUES (#{threadId}, #{mediaFileId}, #{sortOrder}, #{now})
		ON CONFLICT (thread_id, media_file_id) DO UPDATE SET sort_order = EXCLUDED.sort_order
		""")
	void insert(
		@Param("threadId") UUID threadId,
		@Param("mediaFileId") UUID mediaFileId,
		@Param("sortOrder") int sortOrder,
		@Param("now") Instant now
	);

	/**
	 * 쓰레드의 첨부 미디어 식별자를 노출 순서대로 조회한다.
	 *
	 * @param threadId 쓰레드 식별자
	 * @return 미디어 파일 식별자 목록
	 */
	@Select("""
		SELECT media_file_id
		FROM community.thread_media
		WHERE thread_id = #{threadId}
		ORDER BY sort_order, media_file_id
		""")
	List<UUID> findMediaFileIdsByThreadId(@Param("threadId") UUID threadId);

	/**
	 * 쓰레드의 첨부 미디어 연결을 모두 제거한다. 수정 시 전체 교체에 사용한다.
	 *
	 * <p>media.media_files의 실제 파일은 삭제하지 않는다.
	 *
	 * @param threadId 쓰레드 식별자
	 */
	@Delete("DELETE FROM community.thread_media WHERE thread_id = #{threadId}")
	void deleteByThreadId(@Param("threadId") UUID threadId);
}

package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.domain.model.ModerationActionRecord;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * {@code community.moderation_actions} 테이블 mapper.
 *
 * <p>모더레이터가 수행한 조치(HIDE, RESTORE, DELETE) 이력을 저장하고 조회한다.
 */
@Mapper
public interface ModerationActionMapper {

	/**
	 * 조치 이력을 등록한다.
	 *
	 * @param id 조치 식별자
	 * @param moderatorUserId 모더레이터
	 * @param targetType 대상 유형
	 * @param targetId 대상 식별자
	 * @param action 조치 유형
	 * @param moderationStatus 결과 moderation status (nullable)
	 * @param moderationReason 조치 사유 (nullable)
	 * @param reportId 연관 신고 (nullable)
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.moderation_actions
		    (id, moderator_user_id, target_type, target_id, action,
		     moderation_status, moderation_reason, report_id, created_at)
		VALUES
		    (#{id}, #{moderatorUserId}, #{targetType}, #{targetId}, #{action},
		     #{moderationStatus}, #{moderationReason}, #{reportId}, #{now})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("moderatorUserId") UUID moderatorUserId,
		@Param("targetType") ReportTargetType targetType,
		@Param("targetId") UUID targetId,
		@Param("action") ModerationActionType action,
		@Param("moderationStatus") ModerationStatus moderationStatus,
		@Param("moderationReason") String moderationReason,
		@Param("reportId") UUID reportId,
		@Param("now") Instant now
	);

	/**
	 * 전체 조치 이력을 최신순으로 페이지네이션한다.
	 *
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return 조치 이력 목록
	 */
	@Select("""
		SELECT id, moderator_user_id, target_type, target_id, action,
		       moderation_status, moderation_reason, report_id, created_at
		FROM community.moderation_actions
		ORDER BY created_at DESC
		LIMIT #{size} OFFSET #{offset}
		""")
	List<ModerationActionRecord> findAll(@Param("offset") int offset, @Param("size") int size);

	/**
	 * 전체 조치 이력 건수를 센다.
	 *
	 * @return 건수
	 */
	@Select("SELECT COUNT(*) FROM community.moderation_actions")
	int countAll();
}

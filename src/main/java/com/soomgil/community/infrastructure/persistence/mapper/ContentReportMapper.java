package com.soomgil.community.infrastructure.persistence.mapper;

import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.domain.model.ContentReportRecord;
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
 * {@code community.content_reports} 테이블 mapper.
 *
 * <p>사용자 신고를 저장하고 모더레이터가 조회·처리한다.
 */
@Mapper
public interface ContentReportMapper {

	/**
	 * 신고를 등록한다. {@code status}는 항상 {@code OPEN}으로 시작한다.
	 *
	 * @param id 신고 식별자
	 * @param reporterUserId 신고자
	 * @param targetType 대상 유형
	 * @param targetId 대상 식별자
	 * @param reasonCode 사유 코드
	 * @param detail 상세 설명 (nullable)
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO community.content_reports
		    (id, reporter_user_id, target_type, target_id, reason_code, detail, status, created_at)
		VALUES
		    (#{id}, #{reporterUserId}, #{targetType}, #{targetId}, #{reasonCode}, #{detail}, 'OPEN', #{now})
		""")
	void insert(
		@Param("id") UUID id,
		@Param("reporterUserId") UUID reporterUserId,
		@Param("targetType") ReportTargetType targetType,
		@Param("targetId") UUID targetId,
		@Param("reasonCode") String reasonCode,
		@Param("detail") String detail,
		@Param("now") Instant now
	);

	/**
	 * 식별자로 신고를 조회한다.
	 *
	 * @param id 신고 식별자
	 * @return 신고 레코드. 없으면 empty.
	 */
	@Select("""
		SELECT id, reporter_user_id, target_type, target_id, reason_code, detail,
		       status, resolution_note, resolved_by, resolved_at, created_at
		FROM community.content_reports
		WHERE id = #{id}
		""")
	Optional<ContentReportRecord> findById(@Param("id") UUID id);

	/**
	 * 동일 신고자가 동일 대상에 대해 미해결(OPEN 또는 REVIEWING) 신고가 있는지 확인한다.
	 *
	 * @param reporterUserId 신고자
	 * @param targetType 대상 유형
	 * @param targetId 대상 식별자
	 * @return 중복 신고 존재 여부
	 */
	@Select("""
		SELECT EXISTS(
		    SELECT 1 FROM community.content_reports
		    WHERE reporter_user_id = #{reporterUserId}
		      AND target_type = #{targetType}
		      AND target_id = #{targetId}
		      AND status IN ('OPEN', 'REVIEWING')
		)
		""")
	boolean existsOpenByReporterAndTarget(
		@Param("reporterUserId") UUID reporterUserId,
		@Param("targetType") ReportTargetType targetType,
		@Param("targetId") UUID targetId
	);

	/**
	 * 상태별 신고 목록을 페이지네이션한다.
	 *
	 * @param status 필터링할 상태 (null이면 전체)
	 * @param offset 건너뛸 row 수
	 * @param size 가져올 row 수
	 * @return 신고 목록
	 */
	@Select("""
		<script>
		SELECT id, reporter_user_id, target_type, target_id, reason_code, detail,
		       status, resolution_note, resolved_by, resolved_at, created_at
		FROM community.content_reports
		<if test="status != null">WHERE status = #{status}</if>
		ORDER BY created_at DESC
		LIMIT #{size} OFFSET #{offset}
		</script>
		""")
	List<ContentReportRecord> findByStatus(
		@Param("status") ReportStatus status,
		@Param("offset") int offset,
		@Param("size") int size
	);

	/**
	 * 상태별 신고 수를 센다.
	 *
	 * @param status 필터링할 상태 (null이면 전체)
	 * @return 건수
	 */
	@Select("""
		<script>
		SELECT COUNT(*)
		FROM community.content_reports
		<if test="status != null">WHERE status = #{status}</if>
		</script>
		""")
	int countByStatus(@Param("status") ReportStatus status);

	/**
	 * 신고 상태를 전환하고 처리 정보를 기록한다.
	 *
	 * @param id 신고 식별자
	 * @param status 새 상태
	 * @param resolutionNote 처리 메모 (nullable)
	 * @param resolvedBy 처리자
	 * @param resolvedAt 처리 시각
	 */
	@Update("""
		UPDATE community.content_reports SET
		    status = #{status},
		    resolution_note = #{resolutionNote},
		    resolved_by = #{resolvedBy},
		    resolved_at = #{resolvedAt}
		WHERE id = #{id}
		""")
	void updateResolution(
		@Param("id") UUID id,
		@Param("status") ReportStatus status,
		@Param("resolutionNote") String resolutionNote,
		@Param("resolvedBy") UUID resolvedBy,
		@Param("resolvedAt") Instant resolvedAt
	);
}

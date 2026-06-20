package com.soomgil.planning.infrastructure.persistence.mapper;

import com.soomgil.planning.domain.model.ChecklistMemberStatusRecord;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

/**
 * planning.checklist_item_member_statuses 테이블 접근 mapper.
 *
 * <p>{@code (checklist_item_id, user_id)} 복합 PK. 첫 토글 시 INSERT,
 * 이후 UPDATE는 {@code WHERE checklist_item_id = ? AND user_id = ?}로 식별한다.
 * DBML에 version 컬럼이 없으므로 optimistic lock은 리소스 단위가 아닌
 * 상위 itinerary_version으로 관리된다.
 */
@Mapper
public interface ChecklistMemberStatusMapper {

	/**
	 * item의 모든 멤버 상태를 조회한다.
	 *
	 * @param checklistItemId item 식별자
	 * @return 멤버 상태 목록
	 */
	@Select("""
		SELECT checklist_item_id, user_id, is_completed, completed_at, updated_by_user_id, updated_at
		FROM planning.checklist_item_member_statuses
		WHERE checklist_item_id = #{checklistItemId}
		""")
	List<ChecklistMemberStatusRecord> findByItemId(@Param("checklistItemId") UUID checklistItemId);

	/**
	 * (item, user) 조합으로 멤버 상태를 찾는다.
	 *
	 * @param checklistItemId item 식별자
	 * @param userId 사용자 식별자
	 * @return 멤버 상태. 없으면 empty (first touch)
	 */
	@Select("""
		SELECT checklist_item_id, user_id, is_completed, completed_at, updated_by_user_id, updated_at
		FROM planning.checklist_item_member_statuses
		WHERE checklist_item_id = #{checklistItemId} AND user_id = #{userId}
		""")
	Optional<ChecklistMemberStatusRecord> findByItemIdAndUserId(
		@Param("checklistItemId") UUID checklistItemId,
		@Param("userId") UUID userId
	);

	/**
	 * 신규 멤버 상태를 INSERT한다. 첫 토글 시 호출.
	 *
	 * @param checklistItemId item 식별자
	 * @param userId 사용자 식별자
	 * @param isCompleted 완료 여부
	 * @param completedAt 완료 시각 (isCompleted=false이면 null)
	 * @param actorUserId 수정자 (보통 user_id와 동일)
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.checklist_item_member_statuses (
		    checklist_item_id, user_id, is_completed, completed_at, updated_by_user_id, updated_at
		) VALUES (
		    #{checklistItemId}, #{userId}, #{isCompleted}, #{completedAt}, #{actorUserId}, #{now}
		)
		""")
	void insert(
		@Param("checklistItemId") UUID checklistItemId,
		@Param("userId") UUID userId,
		@Param("isCompleted") boolean isCompleted,
		@Param("completedAt") Instant completedAt,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * 멤버 상태를 갱신한다.
	 *
	 * @param checklistItemId item 식별자
	 * @param userId 사용자 식별자
	 * @param isCompleted 새 완료 여부
	 * @param completedAt 완료 시각 (isCompleted=false이면 null)
	 * @param actorUserId 수정자 (보통 user_id와 동일)
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 대상이 없음
	 */
	@Update("""
		UPDATE planning.checklist_item_member_statuses
		SET is_completed = #{isCompleted},
		    completed_at = #{completedAt},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE checklist_item_id = #{checklistItemId} AND user_id = #{userId}
		""")
	int updateStatus(
		@Param("checklistItemId") UUID checklistItemId,
		@Param("userId") UUID userId,
		@Param("isCompleted") boolean isCompleted,
		@Param("completedAt") Instant completedAt,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);
}

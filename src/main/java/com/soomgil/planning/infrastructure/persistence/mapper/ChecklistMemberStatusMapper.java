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
 * planning.checklist_item_member_status 테이블 접근 mapper.
 *
 * <p>{@code (item_id, user_id)} 복합 PK. 첫 토글 시 INSERT({@code version=1}),
 * 이후 UPDATE는 {@code WHERE item_id = ? AND user_id = ? AND version = ?}로 검증한다.
 */
@Mapper
public interface ChecklistMemberStatusMapper {

	/**
	 * item의 모든 멤버 상태를 조회한다.
	 *
	 * @param itemId item 식별자
	 * @return 멤버 상태 목록
	 */
	@Select("""
		SELECT item_id, user_id, is_completed, completed_at, version, updated_at
		FROM planning.checklist_item_member_status
		WHERE item_id = #{itemId}
		""")
	List<ChecklistMemberStatusRecord> findByItemId(@Param("itemId") UUID itemId);

	/**
	 * (item, user) 조합으로 멤버 상태를 찾는다.
	 *
	 * @param itemId item 식별자
	 * @param userId 사용자 식별자
	 * @return 멤버 상태. 없으면 empty (first touch)
	 */
	@Select("""
		SELECT item_id, user_id, is_completed, completed_at, version, updated_at
		FROM planning.checklist_item_member_status
		WHERE item_id = #{itemId} AND user_id = #{userId}
		""")
	Optional<ChecklistMemberStatusRecord> findByItemIdAndUserId(
		@Param("itemId") UUID itemId,
		@Param("userId") UUID userId
	);

	/**
	 * 신규 멤버 상태를 INSERT한다. 첫 토글 시 호출. version=1.
	 *
	 * @param itemId item 식별자
	 * @param userId 사용자 식별자
	 * @param isCompleted 완료 여부
	 * @param completedAt 완료 시각 (isCompleted=false이면 null)
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.checklist_item_member_status (
		    item_id, user_id, is_completed, completed_at, version, updated_at
		) VALUES (
		    #{itemId}, #{userId}, #{isCompleted}, #{completedAt}, 1, #{now}
		)
		""")
	void insert(
		@Param("itemId") UUID itemId,
		@Param("userId") UUID userId,
		@Param("isCompleted") boolean isCompleted,
		@Param("completedAt") Instant completedAt,
		@Param("now") Instant now
	);

	/**
	 * 멤버 상태를 갱신하고 version을 1 증가시킨다.
	 *
	 * @param itemId item 식별자
	 * @param userId 사용자 식별자
	 * @param isCompleted 새 완료 여부
	 * @param completedAt 완료 시각 (isCompleted=false이면 null)
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.checklist_item_member_status
		SET is_completed = #{isCompleted},
		    completed_at = #{completedAt},
		    version = version + 1,
		    updated_at = #{now}
		WHERE item_id = #{itemId} AND user_id = #{userId} AND version = #{baseVersion}
		""")
	int updateStatus(
		@Param("itemId") UUID itemId,
		@Param("userId") UUID userId,
		@Param("isCompleted") boolean isCompleted,
		@Param("completedAt") Instant completedAt,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);
}

package com.soomgil.planning.infrastructure.persistence.mapper;

import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.domain.model.ChecklistRecord;
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
 * planning.checklists 테이블 접근 mapper.
 *
 * <p>DBML에 version 컬럼이 없으므로 UPDATE/DELETE는 식별자 기반으로 처리한다.
 */
@Mapper
public interface ChecklistMapper {

	/**
	 * 신규 checklist를 INSERT한다.
	 *
	 * @param id checklist 식별자
	 * @param tripId 여행방 식별자
	 * @param scopeType scope
	 * @param itineraryDayId 일차 식별자 (TRIP scope이면 null)
	 * @param title 표시용 제목 (nullable)
	 * @param actorUserId 작성자
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.checklists (
		    id, trip_id, scope_type, itinerary_day_id, title,
		    created_by_user_id, updated_by_user_id, created_at, updated_at
		) VALUES (
		    #{id}, #{tripId}, #{scopeType}, #{itineraryDayId}, #{title},
		    #{actorUserId}, #{actorUserId}, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId,
		@Param("title") String title,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * (trip, scope, day) 조합으로 활성 checklist를 찾는다.
	 *
	 * @param tripId 여행방 식별자
	 * @param scopeType scope
	 * @param itineraryDayId 일차 식별자
	 * @return checklist. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, scope_type, itinerary_day_id, title,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
		       deleted_at, created_at, updated_at
		FROM planning.checklists
		WHERE trip_id = #{tripId}
		  AND scope_type = #{scopeType}
		  AND (itinerary_day_id = #{itineraryDayId}
		       OR (itinerary_day_id IS NULL AND #{itineraryDayId} IS NULL))
		  AND deleted_at IS NULL
		""")
	Optional<ChecklistRecord> findByTripScopeDay(
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId
	);

	/**
	 * trip의 checklist를 조회한다. scope/day 필터는 optional.
	 *
	 * @param tripId 여행방 식별자
	 * @param scopeType scope (null이면 전체)
	 * @param itineraryDayId 일차 식별자 (null이면 전체)
	 * @return 활성 checklist 목록
	 */
	@Select("""
		<script>
		SELECT id, trip_id, scope_type, itinerary_day_id, title,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
		       deleted_at, created_at, updated_at
		FROM planning.checklists
		WHERE trip_id = #{tripId}
		  AND deleted_at IS NULL
		<if test="scopeType != null">AND scope_type = #{scopeType}</if>
		<if test="itineraryDayId != null">AND itinerary_day_id = #{itineraryDayId}</if>
		ORDER BY scope_type, COALESCE(itinerary_day_id, '00000000-0000-0000-0000-000000000000'::uuid)
		</script>
		""")
	List<ChecklistRecord> findByTripIdWithFilters(
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId
	);

	/**
	 * 식별자로 checklist를 조회한다. 삭제된 row도 포함.
	 *
	 * @param id checklist 식별자
	 * @return checklist. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, scope_type, itinerary_day_id, title,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
		       deleted_at, created_at, updated_at
		FROM planning.checklists
		WHERE id = #{id}
		""")
	Optional<ChecklistRecord> findById(@Param("id") UUID id);

	/**
	 * checklist title을 갱신한다.
	 *
	 * @param id checklist 식별자
	 * @param title 새 title (nullable)
	 * @param actorUserId 수정자
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.checklists
		SET title = #{title},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int updateTitle(
		@Param("id") UUID id,
		@Param("title") String title,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * checklist를 soft delete한다.
	 *
	 * @param id checklist 식별자
	 * @param actorUserId 삭제자
	 * @param now 삭제 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.checklists
		SET deleted_at = #{now},
		    deleted_by_user_id = #{actorUserId},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("id") UUID id,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);
}

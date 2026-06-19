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
 * <p>모든 UPDATE/DELETE는 {@code WHERE id = ? AND version = ? AND deleted_at IS NULL} 조건으로
 * 낙관적 잠금을 검증한다.
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
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.checklists (
		    id, trip_id, scope_type, itinerary_day_id, title, version, created_at, updated_at
		) VALUES (
		    #{id}, #{tripId}, #{scopeType}, #{itineraryDayId}, #{title}, 1, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId,
		@Param("title") String title,
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
		SELECT id, trip_id, scope_type, itinerary_day_id, title, version,
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
	 * @return 활성 checklist 목록 (sort_order는 별도로 item에서)
	 */
	@Select("""
		<script>
		SELECT id, trip_id, scope_type, itinerary_day_id, title, version,
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
		SELECT id, trip_id, scope_type, itinerary_day_id, title, version,
		       deleted_at, created_at, updated_at
		FROM planning.checklists
		WHERE id = #{id}
		""")
	Optional<ChecklistRecord> findById(@Param("id") UUID id);

	/**
	 * checklist title을 갱신하고 version을 1 증가시킨다.
	 *
	 * @param id checklist 식별자
	 * @param title 새 title (nullable)
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.checklists
		SET title = #{title}, version = version + 1, updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int updateTitle(
		@Param("id") UUID id,
		@Param("title") String title,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);

	/**
	 * checklist를 soft delete한다.
	 *
	 * @param id checklist 식별자
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 삭제 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.checklists
		SET deleted_at = #{now}, version = version + 1, updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("id") UUID id,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);
}

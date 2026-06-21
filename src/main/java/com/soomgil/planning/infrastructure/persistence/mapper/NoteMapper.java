package com.soomgil.planning.infrastructure.persistence.mapper;

import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.domain.model.NoteRecord;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Update;

/**
 * planning.trip_notes 테이블 접근 mapper.
 *
 * <p>DBML에 version 컬럼이 없으므로 optimistic lock은 리소스 단위가 아닌
 * 상위 itinerary_version으로 관리된다. UPDATE/DELETE는 식별자 기반으로 처리한다.
 */
@Mapper
public interface NoteMapper {

	/**
	 * 신규 note를 INSERT한다.
	 *
	 * @param id note 식별자
	 * @param tripId 여행방 식별자
	 * @param scopeType scope
	 * @param itineraryDayId 일차 식별자 (TRIP scope이면 null)
	 * @param content 본문
	 * @param actorUserId 작성자
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.trip_notes (
		    id, trip_id, scope_type, itinerary_day_id, content,
		    created_by_user_id, updated_by_user_id, created_at, updated_at
		) VALUES (
		    #{id}, #{tripId}, #{scopeType}, #{itineraryDayId}, #{content},
		    #{actorUserId}, #{actorUserId}, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId,
		@Param("content") String content,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * (trip, scope, day) 조합으로 활성 note를 찾는다.
	 *
	 * @param tripId 여행방 식별자
	 * @param scopeType scope
	 * @param itineraryDayId 일차 식별자 (TRIP scope이면 null)
	 * @return note. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, scope_type, itinerary_day_id, content,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
		       deleted_at, created_at, updated_at
		FROM planning.trip_notes
		WHERE trip_id = #{tripId}
		  AND scope_type = #{scopeType}
		  AND itinerary_day_id IS NOT DISTINCT FROM
		      CAST(#{itineraryDayId,jdbcType=OTHER} AS uuid)
		  AND deleted_at IS NULL
		""")
	Optional<NoteRecord> findByTripScopeDay(
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId
	);

	/**
	 * 식별자로 note를 조회한다. 삭제된 row도 포함.
	 *
	 * @param id note 식별자
	 * @return note. 없으면 empty
	 */
	@Select("""
		SELECT id, trip_id, scope_type, itinerary_day_id, content,
		       created_by_user_id, updated_by_user_id, deleted_by_user_id,
		       deleted_at, created_at, updated_at
		FROM planning.trip_notes
		WHERE id = #{id}
		""")
	Optional<NoteRecord> findById(@Param("id") UUID id);

	/**
	 * note 본문을 갱신한다.
	 *
	 * @param id note 식별자
	 * @param content 새 본문
	 * @param actorUserId 수정자
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.trip_notes
		SET content = #{content},
		    updated_by_user_id = #{actorUserId},
		    updated_at = #{now}
		WHERE id = #{id} AND deleted_at IS NULL
		""")
	int updateContent(
		@Param("id") UUID id,
		@Param("content") String content,
		@Param("actorUserId") UUID actorUserId,
		@Param("now") Instant now
	);

	/**
	 * note를 soft delete한다.
	 *
	 * @param id note 식별자
	 * @param actorUserId 삭제자
	 * @param now 삭제 시각
	 * @return 영향받은 row 수. 0이면 대상이 없거나 이미 삭제됨
	 */
	@Update("""
		UPDATE planning.trip_notes
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

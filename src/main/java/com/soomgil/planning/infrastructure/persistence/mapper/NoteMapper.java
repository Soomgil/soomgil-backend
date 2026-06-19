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
 * planning.notes 테이블 접근 mapper.
 *
 * <p>모든 UPDATE/DELETE는 {@code WHERE id = ? AND version = ? AND deleted_at IS NULL} 조건으로
 * 낙관적 잠금을 검증한다. affectedRows가 0이면 버전 충돌이므로 호출부에서
 * {@link com.soomgil.global.error.ErrorCode#PLANNING_VERSION_CONFLICT}를 던진다.
 */
@Mapper
public interface NoteMapper {

	/**
	 * 신규 note를 INSERT한다. version은 1로 시작.
	 *
	 * @param id note 식별자
	 * @param tripId 여행방 식별자
	 * @param scopeType scope
	 * @param itineraryDayId 일차 식별자 (TRIP scope이면 null)
	 * @param content 본문
	 * @param now 생성 시각
	 */
	@Insert("""
		INSERT INTO planning.notes (
		    id, trip_id, scope_type, itinerary_day_id, content, version, created_at, updated_at
		) VALUES (
		    #{id}, #{tripId}, #{scopeType}, #{itineraryDayId}, #{content}, 1, #{now}, #{now}
		)
		""")
	void insert(
		@Param("id") UUID id,
		@Param("tripId") UUID tripId,
		@Param("scopeType") PlanningScopeType scopeType,
		@Param("itineraryDayId") UUID itineraryDayId,
		@Param("content") String content,
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
		SELECT id, trip_id, scope_type, itinerary_day_id, content, version,
		       deleted_at, created_at, updated_at
		FROM planning.notes
		WHERE trip_id = #{tripId}
		  AND scope_type = #{scopeType}
		  AND (itinerary_day_id = #{itineraryDayId}
		       OR (itinerary_day_id IS NULL AND #{itineraryDayId} IS NULL))
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
		SELECT id, trip_id, scope_type, itinerary_day_id, content, version,
		       deleted_at, created_at, updated_at
		FROM planning.notes
		WHERE id = #{id}
		""")
	Optional<NoteRecord> findById(@Param("id") UUID id);

	/**
	 * note 본문을 갱신하고 version을 1 증가시킨다.
	 *
	 * @param id note 식별자
	 * @param content 새 본문
	 * @param baseVersion 호출자가 읽은 version. 충돌 시 0 반환
	 * @param now 수정 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.notes
		SET content = #{content}, version = version + 1, updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int updateContent(
		@Param("id") UUID id,
		@Param("content") String content,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);

	/**
	 * note를 soft delete한다.
	 *
	 * @param id note 식별자
	 * @param baseVersion 호출자가 읽은 version
	 * @param now 삭제 시각
	 * @return 영향받은 row 수. 0이면 버전 충돌
	 */
	@Update("""
		UPDATE planning.notes
		SET deleted_at = #{now}, version = version + 1, updated_at = #{now}
		WHERE id = #{id} AND version = #{baseVersion} AND deleted_at IS NULL
		""")
	int softDelete(
		@Param("id") UUID id,
		@Param("baseVersion") long baseVersion,
		@Param("now") Instant now
	);
}

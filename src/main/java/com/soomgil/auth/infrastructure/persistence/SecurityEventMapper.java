package com.soomgil.auth.infrastructure.persistence;

import com.soomgil.auth.api.dto.SecurityEvent;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * auth.user_security_events 테이블에 대한 MyBatis mapper.
 */
@Mapper
public interface SecurityEventMapper {

	@Select("""
		SELECT id, event_type, success, failure_reason, created_at
		FROM auth.user_security_events
		WHERE user_id = #{userId}
		ORDER BY created_at DESC
		LIMIT #{limit} OFFSET #{offset}
		""")
	List<SecurityEventRow> findByUserId(
		@Param("userId") UUID userId,
		@Param("offset") int offset,
		@Param("limit") int limit
	);

	@Select("SELECT COUNT(*) FROM auth.user_security_events WHERE user_id = #{userId}")
	long countByUserId(@Param("userId") UUID userId);

	/**
	 * DB 행을 나타내는 내부 record. SecurityEvent DTO로 변환한다.
	 */
	record SecurityEventRow(
		Long id,
		String eventType,
		Boolean success,
		String failureReason,
		java.time.Instant createdAt
	) {
		public SecurityEvent toDto() {
			return new SecurityEvent(
				id,
				eventType,
				success,
				failureReason,
				OffsetDateTime.ofInstant(createdAt, ZoneOffset.UTC)
			);
		}
	}
}

package com.soomgil.user.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * {@code auth.users} 테이블의 계정 상태 lifecycle을 갱신하는 MyBatis mapper.
 *
 * <p>user 도메인의 계정 삭제 예약({@code DELETE /me}) 흐름에서만 사용한다.
 * 로그인/상태 조회는 {@code auth.UserMapper}에 그대로 둔다.
 */
@Mapper
public interface UserAccountCommandMapper {

	/**
	 * 계정을 {@code PENDING_DELETION} 상태로 전환하고 삭제 예약 시간을 기록한다.
	 *
	 * <p>MVP 보관 기간(30일) 후 실제 purge는 별도 배치 잡으로 수행한다.
	 * 본 메서드는 상태 전환과 timestamp 기록만 담당한다.
	 *
	 * @param userId 사용자 식별자
	 * @param requestedAt 삭제 요청 시각(보통 {@code now()})
	 * @param scheduledAt 실제 삭제 예정 시각({@code requestedAt + retention})
	 * @return 갱신된 행 수. 정상 흐름에서 1
	 */
	@Update("""
		UPDATE auth.users
		SET status = 'PENDING_DELETION',
		    status_reason = 'User requested account deletion',
		    status_changed_at = now(),
		    deletion_requested_at = #{requestedAt},
		    deletion_scheduled_at = #{scheduledAt},
		    updated_at = now()
		WHERE id = #{userId} AND status = 'ACTIVE'
		""")
	int markPendingDeletion(
		@Param("userId") UUID userId,
		@Param("requestedAt") OffsetDateTime requestedAt,
		@Param("scheduledAt") OffsetDateTime scheduledAt
	);
}

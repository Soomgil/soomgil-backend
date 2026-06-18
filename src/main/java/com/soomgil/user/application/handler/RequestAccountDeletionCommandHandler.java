package com.soomgil.user.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.user.application.command.RequestAccountDeletionCommand;
import com.soomgil.user.application.service.ActiveOwnerTripChecker;
import com.soomgil.user.domain.model.UserException;
import com.soomgil.user.infrastructure.persistence.UserAccountCommandMapper;
import java.time.Duration;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정 삭제 예약을 기록한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>{@link ActiveOwnerTripChecker}로 활성 OWNER 여행방 존재 여부 확인.
 *       있으면 {@code ACCOUNT_DELETION_BLOCKED_BY_ACTIVE_OWNER_TRIP}(409) 예외.</li>
 *   <li>{@code auth.users} row를 {@code PENDING_DELETION} 상태로 전환하고
 *       {@code deletion_requested_at}, {@code deletion_scheduled_at}을 기록.</li>
 *   <li>갱신 행이 0이면 이미 처리됐거나 ACTIVE가 아닌 상태이므로 idempotent하게 성공 처리.</li>
 * </ol>
 *
 * <p>실제 purge는 별도 배치 잡이 {@code deletion_scheduled_at} 이후에 수행한다.
 */
@Component
@Transactional
public class RequestAccountDeletionCommandHandler
	implements CommandHandler<RequestAccountDeletionCommand, NoResult> {

	/**
	 * 계정 삭제 예약 후 실제 purge까지의 보관 기간.
	 *
	 * <p>MVP 정책상 30일. 사용자가 보관 기간 내에 로그인하면 복구 흐름(별도 구현)으로 돌아올 수 있다.
	 */
	public static final Duration DELETION_RETENTION = Duration.ofDays(30);

	private final UserAccountCommandMapper accountCommandMapper;
	private final ActiveOwnerTripChecker activeOwnerTripChecker;

	public RequestAccountDeletionCommandHandler(
		UserAccountCommandMapper accountCommandMapper,
		ActiveOwnerTripChecker activeOwnerTripChecker
	) {
		this.accountCommandMapper = accountCommandMapper;
		this.activeOwnerTripChecker = activeOwnerTripChecker;
	}

	@Override
	public NoResult handle(RequestAccountDeletionCommand command) {
		if (activeOwnerTripChecker.hasActiveOwnerTrip(command.userId())) {
			throw new UserException(ErrorCode.ACCOUNT_DELETION_BLOCKED_BY_ACTIVE_OWNER_TRIP,
				"User " + command.userId() + " owns an active trip.");
		}

		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime scheduledAt = now.plus(DELETION_RETENTION);

		accountCommandMapper.markPendingDeletion(command.userId(), now, scheduledAt);
		return NoResult.INSTANCE;
	}
}

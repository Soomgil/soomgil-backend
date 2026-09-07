package com.soomgil.user.application.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.user.application.command.RequestAccountDeletionCommand;
import com.soomgil.user.infrastructure.persistence.UserAccountCommandMapper;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 계정을 즉시 탈퇴 처리한다.
 *
 * <p>흐름:
 * <ol>
 *   <li>{@code auth.users} 상태를 {@code DELETED}로 전환.</li>
 *   <li>소유 여행방은 다음 활성 구성원에게 넘기고, 혼자라면 삭제한 뒤 모든 여행방에서 탈퇴.</li>
 *   <li>세션과 인증 수단을 폐기하고 이메일·프로필·보안 이벤트의 개인정보를 익명화.</li>
 *   <li>공유 게시물의 참조 무결성을 위해 사용자 ID와 약관 동의 기록은 유지.</li>
 * </ol>
 *
 * <p>모든 변경은 같은 transaction에서 실행된다. 중간 단계가 실패하면 탈퇴 처리 전체가 rollback된다.
 */
@Component
@Transactional
public class RequestAccountDeletionCommandHandler
	implements CommandHandler<RequestAccountDeletionCommand, NoResult> {

	private final UserAccountCommandMapper accountCommandMapper;
	private final TripCommandRepository tripCommandRepository;

	public RequestAccountDeletionCommandHandler(
		UserAccountCommandMapper accountCommandMapper,
		TripCommandRepository tripCommandRepository
	) {
		this.accountCommandMapper = accountCommandMapper;
		this.tripCommandRepository = tripCommandRepository;
	}

	@Override
	public NoResult handle(RequestAccountDeletionCommand command) {
		OffsetDateTime deletedAt = OffsetDateTime.now();
		if (accountCommandMapper.markDeleted(command.userId(), deletedAt) == 0) {
			return NoResult.INSTANCE;
		}

		tripCommandRepository.departUserForAccountDeletion(command.userId(), deletedAt.toInstant());
		accountCommandMapper.revokeSessions(command.userId(), deletedAt);
		accountCommandMapper.deleteEmailVerificationTokens(command.userId());
		accountCommandMapper.deletePasswordResetTokens(command.userId());
		accountCommandMapper.deletePasswordCredential(command.userId());
		accountCommandMapper.deleteOAuthIdentities(command.userId());
		accountCommandMapper.anonymizeEmailAddresses(command.userId(), deletedAt);
		accountCommandMapper.anonymizeProfile(command.userId());
		accountCommandMapper.disableEmailSettings(command.userId(), deletedAt);
		accountCommandMapper.anonymizeSecurityEvents(command.userId());
		return NoResult.INSTANCE;
	}
}

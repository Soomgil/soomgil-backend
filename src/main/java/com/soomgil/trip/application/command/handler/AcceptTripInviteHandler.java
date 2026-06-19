package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.AcceptTripInviteCommand;
import com.soomgil.trip.application.command.dto.AcceptTripInviteResult;
import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripInviteAcceptReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AcceptTripInviteCommand}를 처리해 초대를 수락한다.
 *
 * <p>초대 수락은 멤버십 추가와 초대 상태 변경을 같은 transaction 안에서 수행한다.
 */
@Component
public class AcceptTripInviteHandler implements CommandHandler<AcceptTripInviteCommand, AcceptTripInviteResult> {

	private final TripCommandRepository commandRepository;
	private final TripQueryRepository queryRepository;
	private final TimeProvider timeProvider;

	public AcceptTripInviteHandler(
		TripCommandRepository commandRepository,
		TripQueryRepository queryRepository,
		TimeProvider timeProvider
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public AcceptTripInviteResult handle(AcceptTripInviteCommand command) {
		TripInviteAcceptReadModel invite = queryRepository.findTripInviteForAccept(command.inviteCode())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip invite was not found."));
		Instant now = timeProvider.now();
		validateInvite(invite, command.actorUserId(), now);
		validateMembership(invite, command.actorUserId());

		if (!commandRepository.acceptTripInvite(invite.id(), command.actorUserId(), now)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Trip invite is no longer pending.");
		}

		TripMember member = TripMember.activeMember(
			Ids.newUuid(),
			invite.tripId(),
			command.actorUserId(),
			now
		);
		commandRepository.addTripMember(member);
		return new AcceptTripInviteResult(invite.tripId());
	}

	private void validateInvite(TripInviteAcceptReadModel invite, java.util.UUID actorUserId, Instant now) {
		if (invite.tripStatus() != TripStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip invite was not found.");
		}
		if (invite.status() != InviteStatus.PENDING) {
			throw new BusinessException(ErrorCode.CONFLICT, "Trip invite is not pending.");
		}
		if (invite.expiresAt() != null && !invite.expiresAt().isAfter(now)) {
			throw new BusinessException(ErrorCode.CONFLICT, "Trip invite has expired.");
		}
		if (invite.inviteeUserId() != null && !invite.inviteeUserId().equals(actorUserId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Trip invite is for another user.");
		}
	}

	private void validateMembership(TripInviteAcceptReadModel invite, java.util.UUID actorUserId) {
		TripAccessSnapshot access = queryRepository.findTripAccess(invite.tripId(), actorUserId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found."));
		if (access.tripStatus() != TripStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found.");
		}
		if (access.memberStatus() == TripMemberStatus.ACTIVE) {
			throw new BusinessException(ErrorCode.CONFLICT, "User is already a trip member.");
		}
	}
}

package com.soomgil.trip.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.trip.application.command.dto.CreateTripInviteCommand;
import com.soomgil.trip.application.command.dto.CreateTripInviteResult;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripInviteCodeGenerator;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.query.dto.TripAccessView;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripInvite;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link CreateTripInviteCommand}를 처리해 여행방 초대를 만든다.
 *
 * <p>초대 생성은 owner 전용 작업이다. invite token 원문은 저장하지 않고 SHA-256 hash만 저장한다.
 */
@Component
public class CreateTripInviteHandler implements CommandHandler<CreateTripInviteCommand, CreateTripInviteResult> {

	private final TripCommandRepository commandRepository;
	private final TripAccessGuard accessGuard;
	private final TimeProvider timeProvider;
	private final TripInviteCodeGenerator codeGenerator;

	public CreateTripInviteHandler(
		TripCommandRepository commandRepository,
		TripQueryRepository queryRepository,
		TimeProvider timeProvider,
		TripInviteCodeGenerator codeGenerator
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.accessGuard = new TripAccessGuard(queryRepository);
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.codeGenerator = Objects.requireNonNull(codeGenerator, "codeGenerator must not be null");
	}

	@Override
	@Transactional
	public CreateTripInviteResult handle(CreateTripInviteCommand command) {
		TripAccessView access = accessGuard.requireOwner(command.tripId(), command.actorUserId());
		Instant now = timeProvider.now();
		String inviteCode = codeGenerator.generate();
		TripInvite invite = TripInvite.createPending(
			Ids.newUuid(),
			command.tripId(),
			command.actorUserId(),
			command.inviteeUserId(),
			inviteCode,
			hashInviteToken(inviteCode),
			command.expiresAt(),
			now
		);
		commandRepository.saveTripInvite(invite);
		return new CreateTripInviteResult(
			invite.id(),
			invite.tripId(),
			invite.inviteCode(),
			invite.inviteeUserId(),
			invite.status(),
			invite.expiresAt(),
			invite.createdAt()
		);
	}

	private String hashInviteToken(String token) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hashed);
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 digest is not available", exception);
		}
	}
}

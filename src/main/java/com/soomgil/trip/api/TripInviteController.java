package com.soomgil.trip.api;

import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.api.dto.AcceptTripInviteRequest;
import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripDetail;
import com.soomgil.trip.api.dto.TripMember;
import com.soomgil.trip.api.dto.TripStatus;
import com.soomgil.trip.application.command.dto.AcceptTripInviteCommand;
import com.soomgil.trip.application.command.dto.AcceptTripInviteResult;
import com.soomgil.trip.application.command.handler.AcceptTripInviteHandler;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.user.api.dto.UserSummary;
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 여행방 초대 code 기반 진입 API.
 *
 * <p>여행방 하위 resource가 아니라 초대 code 자체를 path key로 사용하는 endpoint를 담당한다.
 */
@RestController
@RequestMapping("/api/v1/trip-invites")
public class TripInviteController {

	private final AcceptTripInviteHandler acceptTripInviteHandler;
	private final FindTripDetailHandler findTripDetailHandler;
	private final FindDisplayNameQueryHandler displayNameHandler;

	public TripInviteController(
		AcceptTripInviteHandler acceptTripInviteHandler,
		FindTripDetailHandler findTripDetailHandler,
		FindDisplayNameQueryHandler displayNameHandler
	) {
		this.acceptTripInviteHandler = Objects.requireNonNull(
			acceptTripInviteHandler,
			"acceptTripInviteHandler must not be null"
		);
		this.findTripDetailHandler = Objects.requireNonNull(
			findTripDetailHandler,
			"findTripDetailHandler must not be null"
		);
		this.displayNameHandler = Objects.requireNonNull(displayNameHandler, "displayNameHandler must not be null");
	}

	@PostMapping("/{inviteCode}/accept")
	public TripDetail acceptTripInvite(
		@PathVariable String inviteCode,
		@Valid @RequestBody(required = false) AcceptTripInviteRequest request,
		Principal principal
	) {
		UUID currentUserId = currentUserId(principal);
		AcceptTripInviteResult result = acceptTripInviteHandler.handle(new AcceptTripInviteCommand(
			inviteCode,
			currentUserId
		));
		return toTripDetail(findTripDetailHandler.handle(new FindTripDetailQuery(result.tripId(), currentUserId)));
	}

	private UUID currentUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user is required.");
		}
		try {
			return Ids.parseUuid(principal.getName(), "currentUserId");
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED, "Authenticated user id must be a UUID.");
		}
	}

	private TripDetail toTripDetail(TripDetailView view) {
		return new TripDetail(
			view.id(),
			view.title(),
			view.displayDestination(),
			TripStatus.valueOf(view.status().name()),
			TripAccessRole.valueOf(view.myRole().name()),
			view.itineraryVersion(),
			OffsetDateTime.ofInstant(view.createdAt(), ZoneOffset.UTC),
			view.ownerUserId(),
			List.of(),
			view.members().stream().map(this::toTripMember).toList(),
			view.retrippedFromPostId()
		);
	}

	private TripMember toTripMember(TripMemberView view) {
		FindDisplayNameQuery query = new FindDisplayNameQuery(view.userId());
		return new TripMember(
			view.id(),
			view.tripId(),
			new UserSummary(
				view.userId(),
				displayNameHandler.handle(query),
				displayNameHandler.findProfileImageUrl(query)
			),
			com.soomgil.trip.api.dto.TripMemberRole.valueOf(view.role().name()),
			TripAccessRole.valueOf(view.accessRole().name()),
			com.soomgil.trip.api.dto.TripMemberStatus.valueOf(view.status().name()),
			OffsetDateTime.ofInstant(view.joinedAt(), ZoneOffset.UTC)
		);
	}
}

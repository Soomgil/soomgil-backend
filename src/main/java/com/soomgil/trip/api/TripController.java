package com.soomgil.trip.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.CreateTripCommand;
import com.soomgil.trip.application.command.dto.CreateTripResult;
import com.soomgil.trip.application.command.handler.CreateTripHandler;
import com.soomgil.trip.api.dto.AcceptTripInviteRequest;
import com.soomgil.trip.api.dto.CreateTripInviteRequest;
import com.soomgil.trip.api.dto.CreateTripRequest;
import com.soomgil.trip.api.dto.PagedTripSummary;
import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripDetail;
import com.soomgil.trip.api.dto.TripInvite;
import com.soomgil.trip.api.dto.TripMember;
import com.soomgil.trip.api.dto.TripStatus;
import com.soomgil.trip.api.dto.UpdateTripRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips")
public class TripController extends ApiControllerSupport {

	private final CreateTripHandler createTripHandler;

	public TripController(CreateTripHandler createTripHandler) {
		this.createTripHandler = Objects.requireNonNull(createTripHandler, "createTripHandler must not be null");
	}

	@GetMapping
	public PagedTripSummary listTrips(
		@RequestParam(required = false) TripStatus status,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripDetail createTrip(@Valid @RequestBody CreateTripRequest request, Principal principal) {
		UUID currentUserId = currentUserId(principal);
		CreateTripResult result = createTripHandler.handle(new CreateTripCommand(
			currentUserId,
			request.title(),
			request.displayDestination(),
			request.legalRegionCodes()
		));
		return toTripDetail(result);
	}

	@GetMapping("/{tripId}")
	public TripDetail getTrip(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@PatchMapping("/{tripId}")
	public TripDetail updateTrip(@PathVariable UUID tripId, @Valid @RequestBody UpdateTripRequest request) {
		return notImplemented();
	}

	@DeleteMapping("/{tripId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTrip(@PathVariable UUID tripId) {
		notImplemented();
	}

	@GetMapping("/{tripId}/members")
	public List<TripMember> listTripMembers(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@DeleteMapping("/{tripId}/members/{memberId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeTripMember(@PathVariable UUID tripId, @PathVariable UUID memberId) {
		notImplemented();
	}

	@GetMapping("/{tripId}/invites")
	public List<TripInvite> listTripInvites(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@PostMapping("/{tripId}/invites")
	@ResponseStatus(HttpStatus.CREATED)
	public TripInvite createTripInvite(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateTripInviteRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/{tripId}/invites/{inviteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeTripInvite(@PathVariable UUID tripId, @PathVariable UUID inviteId) {
		notImplemented();
	}

	@PostMapping("/invites/accept")
	public TripDetail acceptTripInvite(@Valid @RequestBody AcceptTripInviteRequest request) {
		return notImplemented();
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

	private TripDetail toTripDetail(CreateTripResult result) {
		return new TripDetail(
			result.tripId(),
			result.title(),
			result.displayDestination(),
			TripStatus.valueOf(result.status().name()),
			TripAccessRole.OWNER,
			result.itineraryVersion(),
			OffsetDateTime.ofInstant(result.createdAt(), ZoneOffset.UTC),
			result.ownerUserId(),
			List.of(),
			List.of(),
			null
		);
	}
}

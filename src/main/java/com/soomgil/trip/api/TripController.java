package com.soomgil.trip.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.CreateTripCommand;
import com.soomgil.trip.application.command.dto.CreateTripResult;
import com.soomgil.trip.application.command.handler.CreateTripHandler;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.ListMyTripsQuery;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.PagedTripSummaryView;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.dto.TripSummaryView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.application.query.handler.ListMyTripsHandler;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
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
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.user.api.dto.UserSummary;
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
	private final ListMyTripsHandler listMyTripsHandler;
	private final FindTripDetailHandler findTripDetailHandler;
	private final ListTripMembersHandler listTripMembersHandler;

	public TripController(
		CreateTripHandler createTripHandler,
		ListMyTripsHandler listMyTripsHandler,
		FindTripDetailHandler findTripDetailHandler,
		ListTripMembersHandler listTripMembersHandler
	) {
		this.createTripHandler = Objects.requireNonNull(createTripHandler, "createTripHandler must not be null");
		this.listMyTripsHandler = Objects.requireNonNull(listMyTripsHandler, "listMyTripsHandler must not be null");
		this.findTripDetailHandler = Objects.requireNonNull(findTripDetailHandler, "findTripDetailHandler must not be null");
		this.listTripMembersHandler = Objects.requireNonNull(listTripMembersHandler, "listTripMembersHandler must not be null");
	}

	@GetMapping
	public PagedTripSummary listTrips(
		@RequestParam(required = false) TripStatus status,
		@RequestParam(required = false) TripAccessRole role,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort,
		Principal principal
	) {
		UUID currentUserId = currentUserId(principal);
		PagedTripSummaryView result = listMyTripsHandler.handle(new ListMyTripsQuery(
			currentUserId,
			toDomainStatus(status),
			toDomainRole(role),
			page,
			size,
			sort
		));
		return toPagedTripSummary(result);
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
	public TripDetail getTrip(@PathVariable UUID tripId, Principal principal) {
		UUID currentUserId = currentUserId(principal);
		return toTripDetail(findTripDetailHandler.handle(new FindTripDetailQuery(tripId, currentUserId)));
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
	public List<TripMember> listTripMembers(
		@PathVariable UUID tripId,
		@RequestParam(required = false) com.soomgil.trip.api.dto.TripMemberStatus status,
		Principal principal
	) {
		UUID currentUserId = currentUserId(principal);
		return listTripMembersHandler.handle(new ListTripMembersQuery(
			tripId,
			currentUserId,
			toDomainMemberStatus(status)
		)).stream()
			.map(this::toTripMember)
			.toList();
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
			List.of(new TripMember(
				result.ownerMemberId(),
				result.tripId(),
				userSummary(result.ownerUserId()),
				com.soomgil.trip.api.dto.TripMemberRole.MEMBER,
				TripAccessRole.OWNER,
				com.soomgil.trip.api.dto.TripMemberStatus.ACTIVE,
				OffsetDateTime.ofInstant(result.createdAt(), ZoneOffset.UTC)
			)),
			null
		);
	}

	private PagedTripSummary toPagedTripSummary(PagedTripSummaryView page) {
		return new PagedTripSummary(
			page.items().stream().map(this::toTripSummary).toList(),
			new PageMeta(page.page(), page.size(), page.totalElements(), page.totalPages(), page.sort())
		);
	}

	private com.soomgil.trip.api.dto.TripSummary toTripSummary(TripSummaryView view) {
		return new com.soomgil.trip.api.dto.TripSummary(
			view.id(),
			view.title(),
			view.displayDestination(),
			TripStatus.valueOf(view.status().name()),
			TripAccessRole.valueOf(view.myRole().name()),
			view.itineraryVersion(),
			OffsetDateTime.ofInstant(view.createdAt(), ZoneOffset.UTC)
		);
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
		return new TripMember(
			view.id(),
			view.tripId(),
			userSummary(view.userId()),
			com.soomgil.trip.api.dto.TripMemberRole.valueOf(view.role().name()),
			TripAccessRole.valueOf(view.accessRole().name()),
			com.soomgil.trip.api.dto.TripMemberStatus.valueOf(view.status().name()),
			OffsetDateTime.ofInstant(view.joinedAt(), ZoneOffset.UTC)
		);
	}

	private UserSummary userSummary(UUID userId) {
		return new UserSummary(userId, userId.toString(), null);
	}

	private com.soomgil.trip.domain.model.TripStatus toDomainStatus(TripStatus status) {
		return status == null ? null : com.soomgil.trip.domain.model.TripStatus.valueOf(status.name());
	}

	private com.soomgil.trip.domain.model.TripAccessRole toDomainRole(TripAccessRole role) {
		return role == null ? null : com.soomgil.trip.domain.model.TripAccessRole.valueOf(role.name());
	}

	private com.soomgil.trip.domain.model.TripMemberStatus toDomainMemberStatus(
		com.soomgil.trip.api.dto.TripMemberStatus status
	) {
		return status == null ? null : com.soomgil.trip.domain.model.TripMemberStatus.valueOf(status.name());
	}
}

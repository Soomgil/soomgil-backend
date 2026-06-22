package com.soomgil.trip.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.command.dto.CreateTripCommand;
import com.soomgil.trip.application.command.dto.CreateTripResult;
import com.soomgil.trip.application.command.dto.CreateTripInviteCommand;
import com.soomgil.trip.application.command.dto.CreateTripInviteResult;
import com.soomgil.trip.application.command.dto.DeleteTripCommand;
import com.soomgil.trip.application.command.dto.RemoveTripMemberCommand;
import com.soomgil.trip.application.command.dto.RevokeTripInviteCommand;
import com.soomgil.trip.application.command.dto.UpdateTripCommand;
import com.soomgil.trip.application.command.handler.CreateTripHandler;
import com.soomgil.trip.application.command.handler.CreateTripInviteHandler;
import com.soomgil.trip.application.command.handler.DeleteTripHandler;
import com.soomgil.trip.application.command.handler.RemoveTripMemberHandler;
import com.soomgil.trip.application.command.handler.RevokeTripInviteHandler;
import com.soomgil.trip.application.command.handler.UpdateTripHandler;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.ListMyTripsQuery;
import com.soomgil.trip.application.query.dto.ListTripInvitesQuery;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.PagedTripSummaryView;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripInviteView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.dto.TripSummaryView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.application.query.handler.ListTripInvitesHandler;
import com.soomgil.trip.application.query.handler.ListMyTripsHandler;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
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
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
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
	private final CreateTripInviteHandler createTripInviteHandler;
	private final RevokeTripInviteHandler revokeTripInviteHandler;
	private final UpdateTripHandler updateTripHandler;
	private final DeleteTripHandler deleteTripHandler;
	private final RemoveTripMemberHandler removeTripMemberHandler;
	private final ListMyTripsHandler listMyTripsHandler;
	private final FindTripDetailHandler findTripDetailHandler;
	private final ListTripMembersHandler listTripMembersHandler;
	private final ListTripInvitesHandler listTripInvitesHandler;
	private final FindDisplayNameQueryHandler displayNameHandler;

	public TripController(
		CreateTripHandler createTripHandler,
		CreateTripInviteHandler createTripInviteHandler,
		RevokeTripInviteHandler revokeTripInviteHandler,
		UpdateTripHandler updateTripHandler,
		DeleteTripHandler deleteTripHandler,
		RemoveTripMemberHandler removeTripMemberHandler,
		ListMyTripsHandler listMyTripsHandler,
		FindTripDetailHandler findTripDetailHandler,
		ListTripMembersHandler listTripMembersHandler,
		ListTripInvitesHandler listTripInvitesHandler,
		FindDisplayNameQueryHandler displayNameHandler
	) {
		this.createTripHandler = Objects.requireNonNull(createTripHandler, "createTripHandler must not be null");
		this.createTripInviteHandler = Objects.requireNonNull(createTripInviteHandler, "createTripInviteHandler must not be null");
		this.revokeTripInviteHandler = Objects.requireNonNull(revokeTripInviteHandler, "revokeTripInviteHandler must not be null");
		this.updateTripHandler = Objects.requireNonNull(updateTripHandler, "updateTripHandler must not be null");
		this.deleteTripHandler = Objects.requireNonNull(deleteTripHandler, "deleteTripHandler must not be null");
		this.removeTripMemberHandler = Objects.requireNonNull(removeTripMemberHandler, "removeTripMemberHandler must not be null");
		this.listMyTripsHandler = Objects.requireNonNull(listMyTripsHandler, "listMyTripsHandler must not be null");
		this.findTripDetailHandler = Objects.requireNonNull(findTripDetailHandler, "findTripDetailHandler must not be null");
		this.listTripMembersHandler = Objects.requireNonNull(listTripMembersHandler, "listTripMembersHandler must not be null");
		this.listTripInvitesHandler = Objects.requireNonNull(listTripInvitesHandler, "listTripInvitesHandler must not be null");
		this.displayNameHandler = Objects.requireNonNull(displayNameHandler, "displayNameHandler must not be null");
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
	public TripDetail updateTrip(
		@PathVariable UUID tripId,
		@Valid @RequestBody UpdateTripRequest request,
		Principal principal
	) {
		UUID currentUserId = currentUserId(principal);
		updateTripHandler.handle(new UpdateTripCommand(
			tripId,
			currentUserId,
			request.title(),
			request.displayDestination(),
			request.legalRegionCodes(),
			toDomainStatus(request.status())
		));
		return toTripDetail(findTripDetailHandler.handle(new FindTripDetailQuery(tripId, currentUserId)));
	}

	@DeleteMapping("/{tripId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteTrip(@PathVariable UUID tripId, Principal principal) {
		UUID currentUserId = currentUserId(principal);
		deleteTripHandler.handle(new DeleteTripCommand(tripId, currentUserId));
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

	@DeleteMapping("/{tripId}/members/{userId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removeTripMember(@PathVariable UUID tripId, @PathVariable UUID userId, Principal principal) {
		UUID currentUserId = currentUserId(principal);
		removeTripMemberHandler.handle(new RemoveTripMemberCommand(tripId, userId, currentUserId));
	}

	@GetMapping("/{tripId}/invites")
	public List<TripInvite> listTripInvites(
		@PathVariable UUID tripId,
		@RequestParam(required = false) com.soomgil.trip.api.dto.InviteStatus status,
		Principal principal
	) {
		UUID currentUserId = currentUserId(principal);
		return listTripInvitesHandler.handle(new ListTripInvitesQuery(
			tripId,
			currentUserId,
			toDomainInviteStatus(status)
		)).stream()
			.map(this::toTripInvite)
			.toList();
	}

	@PostMapping("/{tripId}/invites")
	@ResponseStatus(HttpStatus.CREATED)
	public TripInvite createTripInvite(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateTripInviteRequest request,
		Principal principal
	) {
		UUID currentUserId = currentUserId(principal);
		CreateTripInviteResult result = createTripInviteHandler.handle(new CreateTripInviteCommand(
			tripId,
			currentUserId,
			request.inviteeUserId(),
			request.expiresAt() == null ? null : request.expiresAt().toInstant()
		));
		return toTripInvite(result);
	}

	@DeleteMapping("/{tripId}/invites/{inviteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeTripInvite(@PathVariable UUID tripId, @PathVariable UUID inviteId, Principal principal) {
		UUID currentUserId = currentUserId(principal);
		revokeTripInviteHandler.handle(new RevokeTripInviteCommand(tripId, inviteId, currentUserId));
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

	private TripInvite toTripInvite(CreateTripInviteResult result) {
		return new TripInvite(
			result.id(),
			result.tripId(),
			result.inviteCode(),
			null,
			result.inviteeUserId(),
			com.soomgil.trip.api.dto.InviteStatus.valueOf(result.status().name()),
			result.expiresAt() == null ? null : OffsetDateTime.ofInstant(result.expiresAt(), ZoneOffset.UTC),
			OffsetDateTime.ofInstant(result.createdAt(), ZoneOffset.UTC)
		);
	}

	private TripInvite toTripInvite(TripInviteView view) {
		return new TripInvite(
			view.id(),
			view.tripId(),
			view.inviteCode(),
			null,
			view.inviteeUserId(),
			com.soomgil.trip.api.dto.InviteStatus.valueOf(view.status().name()),
			view.expiresAt() == null ? null : OffsetDateTime.ofInstant(view.expiresAt(), ZoneOffset.UTC),
			OffsetDateTime.ofInstant(view.createdAt(), ZoneOffset.UTC)
		);
	}

	private UserSummary userSummary(UUID userId) {
		FindDisplayNameQuery query = new FindDisplayNameQuery(userId);
		return new UserSummary(
			userId,
			displayNameHandler.handle(query),
			displayNameHandler.findProfileImageUrl(query)
		);
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

	private com.soomgil.trip.domain.model.InviteStatus toDomainInviteStatus(
		com.soomgil.trip.api.dto.InviteStatus status
	) {
		return status == null ? null : com.soomgil.trip.domain.model.InviteStatus.valueOf(status.name());
	}
}

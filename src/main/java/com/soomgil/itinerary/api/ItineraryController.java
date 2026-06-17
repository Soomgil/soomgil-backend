package com.soomgil.itinerary.api;

import com.soomgil.collaboration.api.dto.VersionedCommandRequest;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.api.dto.CreateItineraryDayRequest;
import com.soomgil.itinerary.api.dto.CreateItineraryItemRequest;
import com.soomgil.itinerary.api.dto.CreateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.Itinerary;
import com.soomgil.itinerary.api.dto.ItineraryDay;
import com.soomgil.itinerary.api.dto.ItineraryMutationResponse;
import com.soomgil.itinerary.api.dto.ItineraryItem;
import com.soomgil.itinerary.api.dto.MapMatchRouteRequest;
import com.soomgil.itinerary.api.dto.MapMatchRouteResponse;
import com.soomgil.itinerary.api.dto.ReorderItineraryRequest;
import com.soomgil.itinerary.api.dto.UpdateItineraryDayRequest;
import com.soomgil.itinerary.api.dto.UpdateItineraryItemRequest;
import com.soomgil.itinerary.api.dto.UpdateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.UpdateRouteRequest;
import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryDayView;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import jakarta.validation.Valid;
import java.security.Principal;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/itinerary")
public class ItineraryController extends ApiControllerSupport {

	private final CreateItineraryDayHandler createItineraryDayHandler;
	private final CreateItineraryItemHandler createItineraryItemHandler;

	public ItineraryController(
		CreateItineraryDayHandler createItineraryDayHandler,
		CreateItineraryItemHandler createItineraryItemHandler
	) {
		this.createItineraryDayHandler = Objects.requireNonNull(
			createItineraryDayHandler,
			"createItineraryDayHandler must not be null"
		);
		this.createItineraryItemHandler = Objects.requireNonNull(
			createItineraryItemHandler,
			"createItineraryItemHandler must not be null"
		);
	}

	@GetMapping
	public Itinerary getItinerary(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@PostMapping("/days")
	@ResponseStatus(HttpStatus.CREATED)
	public ItineraryMutationResponse createDay(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateItineraryDayRequest request,
		Principal principal
	) {
		return toResponse(createItineraryDayHandler.handle(new CreateItineraryDayCommand(
			tripId,
			currentUserId(principal),
			request.baseVersion(),
			com.soomgil.itinerary.domain.model.ItineraryDayGroupType.valueOf(request.groupType().name()),
			request.dayNumber(),
			request.date(),
			request.title(),
			request.sortOrder()
		)));
	}

	@PatchMapping("/days/{dayId}")
	public ItineraryMutationResponse updateDay(
		@PathVariable UUID tripId,
		@PathVariable UUID dayId,
		@Valid @RequestBody UpdateItineraryDayRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/days/{dayId}")
	public ItineraryMutationResponse deleteDay(
		@PathVariable UUID tripId,
		@PathVariable UUID dayId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/items")
	@ResponseStatus(HttpStatus.CREATED)
	public ItineraryMutationResponse createItem(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateItineraryItemRequest request,
		Principal principal
	) {
		return toResponse(createItineraryItemHandler.handle(new CreateItineraryItemCommand(
			tripId,
			currentUserId(principal),
			request.baseVersion(),
			request.itineraryDayId(),
			request.sortOrder(),
			com.soomgil.itinerary.domain.model.ItineraryItemType.valueOf(request.itemType().name()),
			request.place() == null ? null : request.place().provider().name(),
			request.place() == null ? null : request.place().externalPlaceId(),
			request.placeName(),
			request.address(),
			request.lat(),
			request.lng(),
			request.thumbnailUrl()
		)));
	}

	@PatchMapping("/items/{itemId}")
	public ItineraryMutationResponse updateItem(
		@PathVariable UUID tripId,
		@PathVariable UUID itemId,
		@Valid @RequestBody UpdateItineraryItemRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/items/{itemId}")
	public ItineraryMutationResponse deleteItem(
		@PathVariable UUID tripId,
		@PathVariable UUID itemId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return notImplemented();
	}

	@PutMapping("/order")
	public ItineraryMutationResponse reorderItinerary(
		@PathVariable UUID tripId,
		@Valid @RequestBody ReorderItineraryRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/routes/map-match")
	public MapMatchRouteResponse mapMatchRoute(
		@PathVariable UUID tripId,
		@Valid @RequestBody MapMatchRouteRequest request
	) {
		return notImplemented();
	}

	@PatchMapping("/routes/{routeId}")
	public ItineraryMutationResponse updateRoute(
		@PathVariable UUID tripId,
		@PathVariable UUID routeId,
		@Valid @RequestBody UpdateRouteRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/routes/{routeId}")
	public ItineraryMutationResponse deleteRoute(
		@PathVariable UUID tripId,
		@PathVariable UUID routeId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/drawings")
	@ResponseStatus(HttpStatus.CREATED)
	public ItineraryMutationResponse createDrawing(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateMapDrawingRequest request
	) {
		return notImplemented();
	}

	@PatchMapping("/drawings/{drawingId}")
	public ItineraryMutationResponse updateDrawing(
		@PathVariable UUID tripId,
		@PathVariable UUID drawingId,
		@Valid @RequestBody UpdateMapDrawingRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/drawings/{drawingId}")
	public ItineraryMutationResponse deleteDrawing(
		@PathVariable UUID tripId,
		@PathVariable UUID drawingId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
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

	private ItineraryMutationResponse toResponse(ItineraryMutationResult result) {
		return new ItineraryMutationResponse(
			result.tripId(),
			result.itineraryVersion(),
			result.day() == null ? null : toDay(result.day()),
			result.item() == null ? null : toItem(result.item()),
			null,
			null,
			result.affectedRouteIds()
		);
	}

	private ItineraryDay toDay(ItineraryDayView view) {
		return new ItineraryDay(
			view.id(),
			view.tripId(),
			com.soomgil.itinerary.api.dto.ItineraryDayGroupType.valueOf(view.groupType().name()),
			view.dayNumber(),
			view.date(),
			view.title(),
			view.sortOrder(),
			List.of()
		);
	}

	private ItineraryItem toItem(ItineraryItemView view) {
		return new ItineraryItem(
			view.id(),
			view.itineraryDayId(),
			view.sortOrder(),
			com.soomgil.itinerary.api.dto.ItineraryItemType.valueOf(view.itemType().name()),
			toPlaceRef(view.placeProvider(), view.externalPlaceId()),
			view.placeName(),
			view.address(),
			view.lat(),
			view.lng(),
			view.thumbnailUrl(),
			PlaceSourceStatus.valueOf(view.sourceStatus())
		);
	}

	private PlaceRef toPlaceRef(String provider, String externalPlaceId) {
		if (provider == null || externalPlaceId == null) {
			return null;
		}
		return new PlaceRef(PlaceProvider.valueOf(provider), externalPlaceId);
	}
}

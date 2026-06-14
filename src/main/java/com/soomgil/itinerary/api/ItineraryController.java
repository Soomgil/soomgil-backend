package com.soomgil.itinerary.api;

import com.soomgil.collaboration.api.dto.VersionedCommandRequest;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.itinerary.api.dto.CreateItineraryDayRequest;
import com.soomgil.itinerary.api.dto.CreateItineraryItemRequest;
import com.soomgil.itinerary.api.dto.CreateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.Itinerary;
import com.soomgil.itinerary.api.dto.ItineraryMutationResponse;
import com.soomgil.itinerary.api.dto.MapMatchRouteRequest;
import com.soomgil.itinerary.api.dto.MapMatchRouteResponse;
import com.soomgil.itinerary.api.dto.ReorderItineraryRequest;
import com.soomgil.itinerary.api.dto.UpdateItineraryDayRequest;
import com.soomgil.itinerary.api.dto.UpdateItineraryItemRequest;
import com.soomgil.itinerary.api.dto.UpdateMapDrawingRequest;
import com.soomgil.itinerary.api.dto.UpdateRouteRequest;
import jakarta.validation.Valid;
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

	@GetMapping
	public Itinerary getItinerary(@PathVariable UUID tripId) {
		return notImplemented();
	}

	@PostMapping("/days")
	@ResponseStatus(HttpStatus.CREATED)
	public ItineraryMutationResponse createDay(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateItineraryDayRequest request
	) {
		return notImplemented();
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
		@Valid @RequestBody CreateItineraryItemRequest request
	) {
		return notImplemented();
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
}

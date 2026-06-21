package com.soomgil.preference.api;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReactionRequest;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.api.dto.SwipeTagStatus;
import com.soomgil.preference.application.command.dto.SavePlaceCommand;
import com.soomgil.preference.application.command.dto.UnsavePlaceCommand;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import com.soomgil.preference.application.command.handler.SavePlaceCommandHandler;
import com.soomgil.preference.application.command.handler.UnsavePlaceCommandHandler;
import com.soomgil.preference.application.command.handler.UpsertSwipeReactionCommandHandler;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.application.query.handler.ListSavedPlacesQueryHandler;
import com.soomgil.preference.application.query.handler.SwipeFeedQueryHandler;
import com.soomgil.preference.application.service.SwipeTagPreparationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SwipeController {

	private final SwipeFeedQueryHandler swipeFeedQueryHandler;
	private final UpsertSwipeReactionCommandHandler upsertSwipeReactionCommandHandler;
	private final SavePlaceCommandHandler savePlaceCommandHandler;
	private final UnsavePlaceCommandHandler unsavePlaceCommandHandler;
	private final ListSavedPlacesQueryHandler listSavedPlacesQueryHandler;
	private final SwipeTagPreparationService swipeTagPreparationService;

	public SwipeController(
		SwipeFeedQueryHandler swipeFeedQueryHandler,
		UpsertSwipeReactionCommandHandler upsertSwipeReactionCommandHandler,
		SavePlaceCommandHandler savePlaceCommandHandler,
		UnsavePlaceCommandHandler unsavePlaceCommandHandler,
		ListSavedPlacesQueryHandler listSavedPlacesQueryHandler,
		SwipeTagPreparationService swipeTagPreparationService
	) {
		this.swipeFeedQueryHandler = swipeFeedQueryHandler;
		this.upsertSwipeReactionCommandHandler = upsertSwipeReactionCommandHandler;
		this.savePlaceCommandHandler = savePlaceCommandHandler;
		this.unsavePlaceCommandHandler = unsavePlaceCommandHandler;
		this.listSavedPlacesQueryHandler = listSavedPlacesQueryHandler;
		this.swipeTagPreparationService = swipeTagPreparationService;
	}

	@GetMapping("/swipe/tags")
	public List<SwipeTagStatus> getSwipeTagStatuses(
		@RequestParam List<String> externalPlaceIds
	) {
		return swipeTagPreparationService.findStatuses(externalPlaceIds);
	}

	@GetMapping("/swipe/feed")
	public SwipeFeedResponse getSwipeFeed(
		@RequestParam(required = false) String legalRegionCode,
		@RequestParam(required = false) String category,
		@RequestParam(defaultValue = "20") int limit,
		@RequestParam(defaultValue = "true") boolean excludeRecent,
		@RequestParam(required = false) String seed
	) {
		return swipeFeedQueryHandler.handle(new SwipeFeedQuery(
			legalRegionCode,
			category,
			limit,
			excludeRecent,
			seed
		));
	}

	@PutMapping("/places/{provider}/{externalPlaceId}/swipe-reaction")
	public SwipeReactionResponse upsertSwipeReaction(
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId,
		@Valid @RequestBody SwipeReactionRequest request
	) {
		return upsertSwipeReactionCommandHandler.handle(new UpsertSwipeReactionCommand(
			provider,
			externalPlaceId,
			request.reaction(),
			null
		));
	}

	@GetMapping("/me/saved-places")
	public PagedSavedPlace listSavedPlaces(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return listSavedPlacesQueryHandler.handle(new ListSavedPlacesQuery(page, size));
	}

	@PutMapping("/places/{provider}/{externalPlaceId}/save")
	public SavedPlace savePlace(
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId
	) {
		return savePlaceCommandHandler.handle(new SavePlaceCommand(provider, externalPlaceId));
	}

	@DeleteMapping("/places/{provider}/{externalPlaceId}/save")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void unsavePlace(
		@PathVariable PlaceProvider provider,
		@PathVariable String externalPlaceId
	) {
		unsavePlaceCommandHandler.handle(new UnsavePlaceCommand(provider, externalPlaceId));
	}
}

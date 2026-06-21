package com.soomgil.preference.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentCommand;
import com.soomgil.preference.application.command.handler.SavePlaceTagEnrichmentCommandHandler;
import com.soomgil.preference.application.command.handler.PreferenceUpsertSwipeReactionCommandHandler;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferencePlaceTagEnrichmentMapper;
import org.springframework.stereotype.Service;

@Service
public class SwipeTagEnrichmentProcessor {

	private static final String PROMPT_VERSION = "swipe-place-tagging-v1";
	private static final String SELECTION_POLICY_VERSION = "preference-selection-v1";
	private final PlaceTagExtractor extractor;
	private final PreferencePlaceTagEnrichmentMapper mapper;
	private final SavePlaceTagEnrichmentCommandHandler saveHandler;
	private final PreferenceUpsertSwipeReactionCommandHandler reactionHandler;

	public SwipeTagEnrichmentProcessor(
		PlaceTagExtractor extractor,
		PreferencePlaceTagEnrichmentMapper mapper,
		SavePlaceTagEnrichmentCommandHandler saveHandler,
		PreferenceUpsertSwipeReactionCommandHandler reactionHandler
	) {
		this.extractor = extractor;
		this.mapper = mapper;
		this.saveHandler = saveHandler;
		this.reactionHandler = reactionHandler;
	}

	public void process(TourismPlaceFeedItem place, String sourceHash) {
		var candidates = extractor.extract(place, mapper.findSelectableTags());
		saveHandler.handle(new SavePlaceTagEnrichmentCommand(
			"KTO", place.externalPlaceId(), place.sourceModifiedAt(), sourceHash,
			"GOOGLE", "GEMINI", PROMPT_VERSION, SELECTION_POLICY_VERSION, candidates
		));
		reactionHandler.refreshPlaceEnrichment("KTO", place.externalPlaceId(), place.sourceModifiedAt());
	}
}

package com.soomgil.preference.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.application.command.dto.SavePlaceTagCandidateCommand;
import com.soomgil.preference.infrastructure.persistence.row.SelectablePreferenceTagRow;
import java.util.List;

public interface PlaceTagExtractor {
	List<SavePlaceTagCandidateCommand> extract(
		TourismPlaceFeedItem place,
		List<SelectablePreferenceTagRow> allowedTags
	);
}

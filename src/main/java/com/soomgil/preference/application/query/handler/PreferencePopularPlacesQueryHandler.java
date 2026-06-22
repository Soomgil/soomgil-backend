package com.soomgil.preference.application.query.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.place.api.dto.PagedPlaceSummary;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.place.application.query.dto.PopularPlacesQuery;
import com.soomgil.place.application.query.handler.PopularPlacesQueryHandler;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSavedPlaceMapper;
import java.net.URI;
import java.util.List;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferencePopularPlacesQueryHandler implements PopularPlacesQueryHandler {

	private final PreferenceSavedPlaceMapper mapper;

	public PreferencePopularPlacesQueryHandler(PreferenceSavedPlaceMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	@Cacheable(value = "popularPlaces", key = "#query.limit()")
	public PagedPlaceSummary handle(PopularPlacesQuery query) {
		List<PlaceSummary> items = mapper.listPopularPlaces(query.limit())
			.stream()
			.map(row -> new PlaceSummary(
				PlaceProvider.valueOf(row.provider()),
				row.externalPlaceId(),
				row.name(),
				row.address(),
				row.lat(),
				row.lng(),
				toUri(row.thumbnailUrl()),
				row.category(),
				PlaceSourceStatus.AVAILABLE
			))
			.toList();

		return new PagedPlaceSummary(items, new PageMeta(0, query.limit(), (long) items.size(), 1, List.of("savedCount,desc")));
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}
}

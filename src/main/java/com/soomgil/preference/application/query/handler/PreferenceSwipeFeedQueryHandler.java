package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.preference.api.dto.SwipeFeedItem;
import com.soomgil.preference.api.dto.SwipeFeedResponse;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.query.dto.SwipeFeedQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedPlaceRow;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관광 원천 장소를 사용해 개인 스와이프 feed를 조회한다.
 */
@Service
public class PreferenceSwipeFeedQueryHandler implements SwipeFeedQueryHandler {

	private static final int DEFAULT_LIMIT = 20;
	private static final int MAX_LIMIT = 50;

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final PreferenceSwipeFeedMapper mapper;

	public PreferenceSwipeFeedQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSwipeFeedMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	@Override
	public SwipeFeedResponse handle(SwipeFeedQuery query) {
		UUID userId = currentUserId();
		List<SwipeFeedItem> items = mapper.findFeed(
				userId.toString(),
				query.legalRegionCode(),
				query.category(),
				normalizeLimit(query.limit()),
				query.excludeRecent()
			)
			.stream()
			.map(this::toItem)
			.toList();

		return new SwipeFeedResponse(items, null);
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to read swipe feed.");
		}
		return provider.currentUserId();
	}

	private SwipeFeedItem toItem(SwipeFeedPlaceRow row) {
		return new SwipeFeedItem(
			new PlaceSummary(
				PlaceProvider.KTO,
				String.valueOf(row.contentId()),
				row.title(),
				row.address(),
				row.latitude(),
				row.longitude(),
				toUri(row.thumbnailUrl()),
				row.category(),
				PlaceSourceStatus.AVAILABLE
			),
			toReaction(row.myReaction()),
			List.of()
		);
	}

	private SwipeReaction toReaction(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return SwipeReaction.valueOf(value);
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}

	private int normalizeLimit(int limit) {
		if (limit < 1) {
			return DEFAULT_LIMIT;
		}
		return Math.min(limit, MAX_LIMIT);
	}
}

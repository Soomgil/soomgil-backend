package com.soomgil.preference.application.service;

import com.soomgil.place.application.port.TourismPlaceFeedItem;
import com.soomgil.preference.api.dto.TagPreparationStatus;
import com.soomgil.preference.api.dto.SwipeTagStatus;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeFeedMapper;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagRow;
import com.soomgil.preference.infrastructure.persistence.row.SwipeFeedTagStateRow;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class SwipeTagPreparationService {

	private final PreferenceSwipeFeedMapper mapper;
	private final SwipeTagEnrichmentQueue queue;

	public SwipeTagPreparationService(PreferenceSwipeFeedMapper mapper, SwipeTagEnrichmentQueue queue) {
		this.mapper = mapper;
		this.queue = queue;
	}

	public Map<String, SwipeTagPreparation> prepare(List<TourismPlaceFeedItem> places) {
		if (places.isEmpty()) {
			return Map.of();
		}
		List<String> ids = places.stream().map(TourismPlaceFeedItem::externalPlaceId).toList();
		Map<String, SwipeFeedTagStateRow> states = mapper.findTagStates(ids).stream()
			.collect(Collectors.toMap(SwipeFeedTagStateRow::externalPlaceId, Function.identity()));
		Map<String, List<String>> tags = mapper.findTags(ids).stream()
			.collect(Collectors.groupingBy(
				SwipeFeedTagRow::externalPlaceId,
				LinkedHashMap::new,
				Collectors.mapping(SwipeFeedTagRow::displayName, Collectors.toList())
			));

		Map<String, SwipeTagPreparation> result = new LinkedHashMap<>();
		for (TourismPlaceFeedItem place : places) {
			String sourceHash = sourceHash(place);
			SwipeFeedTagStateRow state = states.get(place.externalPlaceId());
			List<String> existingTags = tags.getOrDefault(place.externalPlaceId(), List.of());
			if (isFresh(place, sourceHash, state)) {
				result.put(place.externalPlaceId(), new SwipeTagPreparation(existingTags, TagPreparationStatus.READY));
				continue;
			}
			queue.enqueue(place, sourceHash);
			TagPreparationStatus status = existingTags.isEmpty()
				? TagPreparationStatus.PENDING : TagPreparationStatus.REFRESHING;
			result.put(place.externalPlaceId(), new SwipeTagPreparation(existingTags, status));
		}
		return result;
	}

	public String sourceHash(TourismPlaceFeedItem place) {
		String source = String.join("\u001f",
			value(place.name()), value(place.address()), value(place.category()), value(place.description()),
			value(place.thumbnailUrl()), String.join("\u001e", place.photos())
		);
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(source.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available.", exception);
		}
	}

	public List<SwipeTagStatus> findStatuses(List<String> externalPlaceIds) {
		if (externalPlaceIds == null || externalPlaceIds.isEmpty()) {
			return List.of();
		}
		List<String> ids = externalPlaceIds.stream()
			.filter(id -> id != null && !id.isBlank())
			.distinct()
			.limit(50)
			.toList();
		if (ids.isEmpty()) {
			return List.of();
		}
		Map<String, List<String>> tags = mapper.findTags(ids).stream()
			.collect(Collectors.groupingBy(
				SwipeFeedTagRow::externalPlaceId,
				LinkedHashMap::new,
				Collectors.mapping(SwipeFeedTagRow::displayName, Collectors.toList())
			));
		return ids.stream().map(id -> {
			List<String> existing = tags.getOrDefault(id, List.of());
			TagPreparationStatus status = queue.isQueued(id)
				? (existing.isEmpty() ? TagPreparationStatus.PENDING : TagPreparationStatus.REFRESHING)
				: (existing.isEmpty() ? TagPreparationStatus.PENDING : TagPreparationStatus.READY);
			return new SwipeTagStatus(id, existing, status);
		}).toList();
	}

	private boolean isFresh(TourismPlaceFeedItem place, String sourceHash, SwipeFeedTagStateRow state) {
		if (state == null) {
			return false;
		}
		if (place.sourceModifiedAt() != null && state.sourceModifiedAt() != null) {
			if (place.sourceModifiedAt().isAfter(state.sourceModifiedAt())) {
				return false;
			}
			return state.sourceHash() == null || sourceHash.equals(state.sourceHash());
		}
		return sourceHash.equals(state.sourceHash());
	}

	private String value(Object value) {
		return value == null ? "" : value.toString().trim();
	}
}

package com.soomgil.preference.application.command.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.api.dto.PlaceSummary;
import com.soomgil.preference.api.dto.PagedSavedPlace;
import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.preference.application.command.dto.SavePlaceCommand;
import com.soomgil.preference.application.command.dto.UnsavePlaceCommand;
import com.soomgil.preference.application.query.dto.ListSavedPlacesQuery;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSavedPlaceMapper;
import com.soomgil.preference.infrastructure.persistence.row.SavedPlaceInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.SavedPlaceRow;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 저장 장소 생성, 해제, 목록 조회 공통 로직.
 */
@Service
public class PreferenceSavedPlaceService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final PreferenceSavedPlaceMapper mapper;

	public PreferenceSavedPlaceService(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSavedPlaceMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
	}

	@Transactional
	public SavedPlace save(SavePlaceCommand command) {
		UUID userId = currentUserId();
		String provider = command.provider().name();
		String reaction = mapper.findReaction(userId.toString(), provider, command.externalPlaceId());
		if (!"SUPER_LIKE".equals(reaction)) {
			throw new BusinessException(
				ErrorCode.BUSINESS_RULE_VIOLATION,
				"Place must be SUPER_LIKE before it can be saved."
			);
		}

		String savedPlaceId = mapper.findSavedPlaceId(userId.toString(), provider, command.externalPlaceId());
		if (savedPlaceId == null) {
			savedPlaceId = Ids.newUuid().toString();
			mapper.insertSavedPlace(new SavedPlaceInsertRow(
				savedPlaceId,
				userId.toString(),
				provider,
				command.externalPlaceId()
			));
		}
		else {
			mapper.reactivateSavedPlace(savedPlaceId);
		}

		return toSavedPlace(mapper.findSavedPlaceById(savedPlaceId));
	}

	@Transactional
	public NoResult unsave(UnsavePlaceCommand command) {
		UUID userId = currentUserId();
		int updatedRows = mapper.softDeleteSavedPlace(
			userId.toString(),
			command.provider().name(),
			command.externalPlaceId()
		);
		if (updatedRows == 0) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Saved place was not found.");
		}
		return NoResult.INSTANCE;
	}

	@Transactional(readOnly = true)
	public PagedSavedPlace list(ListSavedPlacesQuery query) {
		UUID userId = currentUserId();
		int page = normalizePage(query.page());
		int size = normalizeSize(query.size());
		long totalElements = mapper.countSavedPlaces(userId.toString());
		List<SavedPlace> items = mapper.listSavedPlaces(userId.toString(), size, page * size)
			.stream()
			.map(this::toSavedPlace)
			.toList();

		return new PagedSavedPlace(items, new PageMeta(
			page,
			size,
			totalElements,
			totalPages(totalElements, size),
			List.of("createdAt,desc")
		));
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to manage saved places.");
		}
		return provider.currentUserId();
	}

	private SavedPlace toSavedPlace(SavedPlaceRow row) {
		return new SavedPlace(
			UUID.fromString(row.id()),
			new PlaceSummary(
				PlaceProvider.valueOf(row.provider()),
				row.externalPlaceId(),
				row.name(),
				row.address(),
				row.lat(),
				row.lng(),
				toUri(row.thumbnailUrl()),
				row.category(),
				PlaceSourceStatus.AVAILABLE
			),
			row.createdAt()
		);
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}

	private int normalizePage(int page) {
		return Math.max(page, 0);
	}

	private int normalizeSize(int size) {
		if (size < 1) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}

	private int totalPages(long totalElements, int size) {
		if (totalElements == 0) {
			return 0;
		}
		return (int) Math.ceil((double) totalElements / size);
	}
}

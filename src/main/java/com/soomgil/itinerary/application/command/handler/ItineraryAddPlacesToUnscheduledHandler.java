package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledCommand;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledResult;
import com.soomgil.itinerary.application.command.dto.AddedUnscheduledPlace;
import com.soomgil.itinerary.application.command.dto.SkippedUnscheduledPlace;
import com.soomgil.itinerary.application.command.dto.UnscheduledPlaceToAdd;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayReadModel;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.itinerary.application.port.ItineraryQueryRepository;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link AddPlacesToUnscheduledCommand}를 처리해 일차 미정 그룹에 장소를 일괄 추가한다.
 *
 * <p>쓰기 handler이므로 transaction 경계를 가진다. 처리 순서는 권한 확인 → 중복 판정 →
 * 일차 미정 그룹 확보 → version 1회 증가 → item 저장 → 협업 이벤트 저장이다.
 *
 * <p>중복 판정은 여행방의 active 일정 item 전체를 대상으로 하므로, 이미 실제 day에 배치된 장소도
 * 다시 추가되지 않는다. 실제로 추가할 장소가 없으면 version을 올리지 않고 일차 미정 그룹도 만들지 않는다.
 * 이 두 성질 덕분에 같은 command를 재시도해도 결과가 중복되지 않는다.
 */
@Component
public class ItineraryAddPlacesToUnscheduledHandler implements AddPlacesToUnscheduledHandler {

	private static final String SOURCE_STATUS_AVAILABLE = "AVAILABLE";
	private static final String UNSCHEDULED_DAY_TITLE = "일차 미정";

	private final ItineraryCommandRepository commandRepository;
	private final ItineraryQueryRepository queryRepository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public ItineraryAddPlacesToUnscheduledHandler(
		ItineraryCommandRepository commandRepository,
		ItineraryQueryRepository queryRepository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Override
	@Transactional
	public AddPlacesToUnscheduledResult handle(AddPlacesToUnscheduledCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());

		long currentVersion = commandRepository.findItineraryVersion(command.tripId())
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip was not found."));

		List<ItineraryItemReadModel> existingItems = queryRepository.findItems(command.tripId());
		Map<PlaceKey, UUID> existingByPlace = indexExistingPlaces(existingItems);

		List<SkippedUnscheduledPlace> skipped = new ArrayList<>();
		List<UnscheduledPlaceToAdd> toInsert = new ArrayList<>();
		Set<PlaceKey> plannedKeys = new LinkedHashSet<>();
		for (UnscheduledPlaceToAdd place : command.places()) {
			PlaceKey key = PlaceKey.of(place.placeProvider(), place.externalPlaceId());
			UUID existingItemId = existingByPlace.get(key);
			if (existingItemId != null) {
				skipped.add(new SkippedUnscheduledPlace(
					place.placeProvider(), place.externalPlaceId(), place.placeName(), existingItemId
				));
				continue;
			}
			if (!plannedKeys.add(key)) {
				continue;
			}
			toInsert.add(place);
		}

		if (toInsert.isEmpty()) {
			return new AddPlacesToUnscheduledResult(
				command.tripId(),
				currentVersion,
				commandRepository.findUnscheduledDay(command.tripId())
					.map(ItineraryDayReadModel::id)
					.orElse(null),
				List.of(),
				skipped
			);
		}

		Instant now = timeProvider.now();
		UUID dayId = resolveUnscheduledDayId(command.tripId(), now);
		int nextSortOrder = nextSortOrder(existingItems, dayId);
		long newVersion = commandRepository.incrementItineraryVersion(command.tripId(), currentVersion, now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));

		List<AddedUnscheduledPlace> added = new ArrayList<>();
		for (UnscheduledPlaceToAdd place : toInsert) {
			ItineraryItemCreate item = new ItineraryItemCreate(
				Ids.newUuid(),
				command.tripId(),
				dayId,
				nextSortOrder++,
				ItineraryItemType.PLACE,
				place.placeProvider(),
				place.externalPlaceId(),
				place.placeName(),
				place.address(),
				place.lat(),
				place.lng(),
				place.thumbnailUrl() == null ? null : place.thumbnailUrl().toString(),
				SOURCE_STATUS_AVAILABLE,
				command.actorUserId(),
				command.actorUserId(),
				now,
				now
			);
			commandRepository.insertItem(item);
			eventRepository.save(ItineraryCollaborationEvents.itemCreated(item, currentVersion, newVersion, now));
			added.add(new AddedUnscheduledPlace(
				item.id(), item.placeProvider(), item.externalPlaceId(), item.placeName()
			));
		}

		return new AddPlacesToUnscheduledResult(command.tripId(), newVersion, dayId, added, skipped);
	}

	private Map<PlaceKey, UUID> indexExistingPlaces(List<ItineraryItemReadModel> items) {
		Map<PlaceKey, UUID> index = new LinkedHashMap<>();
		for (ItineraryItemReadModel item : items) {
			if (item.placeProvider() == null || item.externalPlaceId() == null) {
				continue;
			}
			index.putIfAbsent(PlaceKey.of(item.placeProvider(), item.externalPlaceId()), item.id());
		}
		return index;
	}

	private UUID resolveUnscheduledDayId(UUID tripId, Instant now) {
		Optional<ItineraryDayReadModel> existing = commandRepository.findUnscheduledDay(tripId);
		if (existing.isPresent()) {
			return existing.get().id();
		}
		ItineraryDayCreate day = new ItineraryDayCreate(
			Ids.newUuid(),
			tripId,
			ItineraryDayGroupType.UNSCHEDULED,
			null,
			null,
			UNSCHEDULED_DAY_TITLE,
			(int) commandRepository.countDays(tripId),
			now,
			now
		);
		commandRepository.insertDay(day);
		return day.id();
	}

	private int nextSortOrder(List<ItineraryItemReadModel> existingItems, UUID dayId) {
		return existingItems.stream()
			.filter(item -> dayId.equals(item.itineraryDayId()))
			.map(ItineraryItemReadModel::sortOrder)
			.filter(Objects::nonNull)
			.mapToInt(Integer::intValue)
			.max()
			.orElse(-1) + 1;
	}

	private record PlaceKey(String provider, String externalPlaceId) {

		private static PlaceKey of(String provider, String externalPlaceId) {
			return new PlaceKey(
				provider == null ? null : provider.trim().toUpperCase(java.util.Locale.ROOT),
				externalPlaceId == null ? null : externalPlaceId.trim()
			);
		}
	}
}

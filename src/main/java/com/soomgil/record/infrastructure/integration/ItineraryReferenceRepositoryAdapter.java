package com.soomgil.record.infrastructure.integration;

import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryItemReadModel;
import com.soomgil.record.application.port.ItineraryReferenceRepository;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * 일정 저장소를 이용해 여행 기록의 일정 참조를 확인한다.
 */
@Component
public class ItineraryReferenceRepositoryAdapter implements ItineraryReferenceRepository {

	private final ItineraryCommandRepository itineraryRepository;

	public ItineraryReferenceRepositoryAdapter(ItineraryCommandRepository itineraryRepository) {
		this.itineraryRepository = Objects.requireNonNull(itineraryRepository, "itineraryRepository must not be null");
	}

	@Override
	public boolean existsDay(UUID tripId, UUID dayId) {
		return itineraryRepository.existsDay(tripId, dayId);
	}

	@Override
	public Optional<UUID> findItemDayId(UUID tripId, UUID itemId) {
		return itineraryRepository.findItem(tripId, itemId).map(ItineraryItemReadModel::itineraryDayId);
	}
}

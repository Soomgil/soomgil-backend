package com.soomgil.itinerary.infrastructure.persistence.repository;

import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryDayOrderUpdate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.ItineraryItemOrderUpdate;
import com.soomgil.itinerary.infrastructure.persistence.mapper.ItineraryCommandMapper;
import java.time.Instant;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 일정 day/item 쓰기 repository.
 */
@Repository
public class MyBatisItineraryCommandRepository implements ItineraryCommandRepository {

	private final ItineraryCommandMapper mapper;

	public MyBatisItineraryCommandRepository(ItineraryCommandMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public OptionalLong incrementItineraryVersion(UUID tripId, long baseVersion, Instant updatedAt) {
		Long version = mapper.incrementItineraryVersion(tripId, baseVersion, updatedAt);
		return version == null ? OptionalLong.empty() : OptionalLong.of(version);
	}

	@Override
	public void insertDay(ItineraryDayCreate day) {
		mapper.insertDay(day);
	}

	@Override
	public void insertItem(ItineraryItemCreate item) {
		mapper.insertItem(item);
	}

	@Override
	public boolean existsDay(UUID tripId, UUID dayId) {
		return mapper.existsDay(tripId, dayId);
	}

	@Override
	public boolean existsItem(UUID tripId, UUID itemId) {
		return mapper.existsItem(tripId, itemId);
	}

	@Override
	public void updateDayOrder(ItineraryDayOrderUpdate update) {
		mapper.updateDayOrder(update);
	}

	@Override
	public void updateItemOrder(ItineraryItemOrderUpdate update) {
		mapper.updateItemOrder(update);
	}
}

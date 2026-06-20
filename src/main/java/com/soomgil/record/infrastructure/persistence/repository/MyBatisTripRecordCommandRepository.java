package com.soomgil.record.infrastructure.persistence.repository;

import com.soomgil.record.application.port.TripRecordCommandRepository;
import com.soomgil.record.application.port.TripRecordEntryCreate;
import com.soomgil.record.application.port.TripRecordEntryUpdate;
import com.soomgil.record.infrastructure.persistence.mapper.TripRecordCommandMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 여행 기록 쓰기 repository.
 */
@Repository
public class MyBatisTripRecordCommandRepository implements TripRecordCommandRepository {

	private final TripRecordCommandMapper mapper;

	public MyBatisTripRecordCommandRepository(TripRecordCommandMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public void insertEntry(TripRecordEntryCreate entry) {
		mapper.insertEntry(entry);
	}

	@Override
	public void updateEntry(TripRecordEntryUpdate entry) {
		mapper.updateEntry(entry);
	}

	@Override
	public boolean softDeleteEntry(UUID tripId, UUID recordId, OffsetDateTime deletedAt) {
		return mapper.softDeleteEntry(tripId, recordId, deletedAt) > 0;
	}

	@Override
	public void deleteMediaLinks(UUID recordId) {
		mapper.deleteMediaLinks(recordId);
	}

	@Override
	public void insertMediaLinks(UUID recordId, List<UUID> mediaFileIds, OffsetDateTime createdAt) {
		for (int index = 0; index < mediaFileIds.size(); index++) {
			mapper.insertMediaLink(recordId, mediaFileIds.get(index), index, createdAt);
		}
	}
}

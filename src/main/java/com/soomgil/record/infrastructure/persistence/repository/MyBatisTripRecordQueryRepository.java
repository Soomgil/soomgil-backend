package com.soomgil.record.infrastructure.persistence.repository;

import com.soomgil.record.application.port.TripRecordEntryReadModel;
import com.soomgil.record.application.port.TripRecordMediaReadModel;
import com.soomgil.record.application.port.TripRecordPage;
import com.soomgil.record.application.port.TripRecordPhotoPage;
import com.soomgil.record.application.port.TripRecordQueryRepository;
import com.soomgil.record.infrastructure.persistence.mapper.TripRecordQueryMapper;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 여행 기록 읽기 repository.
 */
@Repository
public class MyBatisTripRecordQueryRepository implements TripRecordQueryRepository {

	private final TripRecordQueryMapper mapper;

	public MyBatisTripRecordQueryRepository(TripRecordQueryMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public TripRecordPage findEntries(UUID tripId, int page, int size) {
		return new TripRecordPage(mapper.findEntries(tripId, size, page * size), mapper.countEntries(tripId));
	}

	@Override
	public Optional<TripRecordEntryReadModel> findEntry(UUID tripId, UUID recordId) {
		return Optional.ofNullable(mapper.findEntry(tripId, recordId));
	}

	@Override
	public List<TripRecordMediaReadModel> findMedia(UUID recordId) {
		return mapper.findMedia(recordId);
	}

	@Override
	public TripRecordPhotoPage findPhotos(UUID tripId, int page, int size) {
		return new TripRecordPhotoPage(mapper.findPhotos(tripId, size, page * size), mapper.countPhotos(tripId));
	}

	@Override
	public TripRecordPhotoPage findPhotosByUser(UUID userId, int page, int size) {
		return new TripRecordPhotoPage(
			mapper.findPhotosByUser(userId, size, page * size),
			mapper.countPhotosByUser(userId)
		);
	}
}

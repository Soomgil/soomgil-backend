package com.soomgil.trip.infrastructure.persistence.repository;

import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.trip.infrastructure.persistence.mapper.TripQueryMapper;
import com.soomgil.trip.infrastructure.persistence.row.TripAccessRow;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 여행방 읽기 repository.
 *
 * <p>권한 계산에 필요한 row를 domain/application 타입으로 변환한다.
 */
@Repository
public class MyBatisTripQueryRepository implements TripQueryRepository {

	private final TripQueryMapper mapper;

	public MyBatisTripQueryRepository(TripQueryMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public Optional<TripAccessSnapshot> findTripAccess(UUID tripId, UUID userId) {
		TripAccessRow row = mapper.findTripAccess(tripId, userId);
		if (row == null) {
			return Optional.empty();
		}
		TripMemberStatus memberStatus = row.memberStatus() == null
			? null
			: TripMemberStatus.valueOf(row.memberStatus());
		return Optional.of(new TripAccessSnapshot(
			row.tripId(),
			row.userId(),
			TripStatus.valueOf(row.tripStatus()),
			memberStatus,
			row.ownerUserId()
		));
	}
}

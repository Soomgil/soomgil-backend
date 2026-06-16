package com.soomgil.trip.infrastructure.persistence.repository;

import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.infrastructure.persistence.mapper.TripCommandMapper;
import com.soomgil.trip.infrastructure.persistence.row.TripMemberRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRegionRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 여행방 쓰기 repository.
 *
 * <p>command handler의 transaction 안에서 여행방, 최초 멤버십, 선택 지역 code를 저장한다.
 */
@Repository
public class MyBatisTripCommandRepository implements TripCommandRepository {

	private final TripCommandMapper mapper;

	public MyBatisTripCommandRepository(TripCommandMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public void saveCreatedTrip(Trip trip, TripMember initialMember, List<String> legalRegionCodes) {
		mapper.insertTrip(new TripRow(
			trip.id(),
			trip.ownerUserId(),
			trip.title(),
			trip.displayDestination(),
			trip.status().name(),
			trip.itineraryVersion(),
			null,
			null,
			trip.createdAt(),
			trip.createdAt(),
			null
		));
		mapper.insertTripMember(new TripMemberRow(
			initialMember.id(),
			initialMember.tripId(),
			initialMember.userId(),
			initialMember.role().name(),
			initialMember.status().name(),
			initialMember.joinedAt(),
			null,
			null
		));

		int order = 0;
		for (String legalRegionCode : new LinkedHashSet<>(legalRegionCodes)) {
			mapper.insertTripRegion(new TripRegionRow(trip.id(), legalRegionCode, order++, trip.createdAt()));
		}
	}
}

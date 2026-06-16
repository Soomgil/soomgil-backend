package com.soomgil.trip.infrastructure.persistence.repository;

import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.application.port.TripSettingsUpdate;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripInvite;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.trip.infrastructure.persistence.mapper.TripCommandMapper;
import com.soomgil.trip.infrastructure.persistence.row.TripInviteRow;
import com.soomgil.trip.infrastructure.persistence.row.TripMemberRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRegionRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
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

	@Override
	public void saveTripInvite(TripInvite invite) {
		mapper.insertTripInvite(new TripInviteRow(
			invite.id(),
			invite.tripId(),
			invite.createdByUserId(),
			invite.inviteeUserId(),
			invite.inviteCode(),
			invite.inviteTokenHash(),
			invite.status().name(),
			invite.expiresAt(),
			invite.acceptedByUserId(),
			invite.acceptedAt(),
			invite.revokedAt(),
			invite.createdAt()
		));
	}

	@Override
	public void revokeTripInvite(UUID inviteId, UUID revokedByUserId, Instant revokedAt) {
		mapper.revokeTripInvite(inviteId, revokedAt);
	}

	@Override
	public void addTripMember(TripMember member) {
		mapper.insertTripMember(new TripMemberRow(
			member.id(),
			member.tripId(),
			member.userId(),
			member.role().name(),
			member.status().name(),
			member.joinedAt(),
			null,
			null
		));
	}

	@Override
	public void acceptTripInvite(UUID inviteId, UUID acceptedByUserId, Instant acceptedAt) {
		mapper.acceptTripInvite(inviteId, acceptedByUserId, acceptedAt);
	}

	@Override
	public void updateTrip(TripSettingsUpdate update) {
		mapper.updateTrip(update);
	}

	@Override
	public void replaceTripRegions(UUID tripId, List<String> legalRegionCodes, Instant createdAt) {
		mapper.deleteTripRegions(tripId);
		int order = 0;
		for (String legalRegionCode : new LinkedHashSet<>(legalRegionCodes)) {
			mapper.insertTripRegion(new TripRegionRow(tripId, legalRegionCode, order++, createdAt));
		}
	}

	@Override
	public void softDeleteTrip(UUID tripId, Instant deletedAt) {
		mapper.softDeleteTrip(tripId, deletedAt);
	}

	@Override
	public void removeTripMember(UUID tripId, UUID userId, UUID removedByUserId, Instant removedAt) {
		mapper.removeTripMember(tripId, userId, removedByUserId, removedAt);
	}
}

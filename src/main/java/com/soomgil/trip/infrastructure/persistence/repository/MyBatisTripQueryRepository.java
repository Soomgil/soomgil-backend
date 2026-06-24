package com.soomgil.trip.infrastructure.persistence.repository;

import com.soomgil.trip.application.port.TripAccessSnapshot;
import com.soomgil.trip.application.port.TripInviteReadModel;
import com.soomgil.trip.application.port.TripInviteAcceptReadModel;
import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripQueryRepository;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.port.TripSummaryPage;
import com.soomgil.trip.domain.model.InviteStatus;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.trip.infrastructure.persistence.mapper.TripQueryMapper;
import com.soomgil.trip.infrastructure.persistence.row.TripAccessRow;
import com.soomgil.trip.infrastructure.persistence.row.TripInviteRow;
import com.soomgil.trip.infrastructure.persistence.row.TripInviteAcceptRow;
import com.soomgil.trip.infrastructure.persistence.row.TripMemberReadRow;
import com.soomgil.trip.infrastructure.persistence.row.TripRow;
import java.util.List;
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

	@Override
	public Optional<TripReadModel> findTrip(UUID tripId) {
		return Optional.ofNullable(mapper.findTrip(tripId)).map(this::toTripReadModel);
	}

	@Override
	public List<TripMemberReadModel> findTripMembers(UUID tripId, TripMemberStatus status) {
		String statusValue = status == null ? null : status.name();
		return mapper.findTripMembers(tripId, statusValue)
			.stream()
			.map(this::toTripMemberReadModel)
			.toList();
	}

	@Override
	public TripSummaryPage findMyTrips(
		UUID userId,
		TripStatus status,
		TripAccessRole role,
		int page,
		int size,
		List<String> sort
	) {
		String statusValue = status == null ? null : status.name();
		String roleValue = role == null ? null : role.name();
		int offset = page * size;
		List<TripReadModel> items = mapper.findMyTrips(userId, statusValue, roleValue, size, offset)
			.stream()
			.map(this::toTripReadModel)
			.toList();
		long totalElements = mapper.countMyTrips(userId, statusValue, roleValue);
		return new TripSummaryPage(items, totalElements);
	}

	@Override
	public List<TripInviteReadModel> findTripInvites(UUID tripId, InviteStatus status) {
		String statusValue = status == null ? null : status.name();
		return mapper.findTripInvites(tripId, statusValue)
			.stream()
			.map(this::toTripInviteReadModel)
			.toList();
	}

	@Override
	public Optional<TripInviteAcceptReadModel> findTripInviteForAccept(String inviteCode) {
		return Optional.ofNullable(mapper.findTripInviteForAccept(inviteCode))
			.map(this::toTripInviteAcceptReadModel);
	}

	private TripReadModel toTripReadModel(TripRow row) {
		return new TripReadModel(
			row.id(),
			row.ownerUserId(),
			row.title(),
			row.displayDestination(),
			TripStatus.valueOf(row.status()),
			row.itineraryVersion(),
			row.startDate(),
			row.endDate(),
			row.createdAt(),
			row.retrippedFromPostId()
		);
	}

	private TripMemberReadModel toTripMemberReadModel(TripMemberReadRow row) {
		return new TripMemberReadModel(
			row.id(),
			row.tripId(),
			row.userId(),
			TripMemberRole.valueOf(row.role()),
			TripMemberStatus.valueOf(row.status()),
			row.joinedAt(),
			row.ownerUserId()
		);
	}

	private TripInviteReadModel toTripInviteReadModel(TripInviteRow row) {
		return new TripInviteReadModel(
			row.id(),
			row.tripId(),
			row.inviteCode(),
			row.inviteeUserId(),
			InviteStatus.valueOf(row.status()),
			row.expiresAt(),
			row.createdAt()
		);
	}

	private TripInviteAcceptReadModel toTripInviteAcceptReadModel(TripInviteAcceptRow row) {
		return new TripInviteAcceptReadModel(
			row.id(),
			row.tripId(),
			row.inviteCode(),
			row.inviteeUserId(),
			InviteStatus.valueOf(row.status()),
			row.expiresAt(),
			row.ownerUserId(),
			TripStatus.valueOf(row.tripStatus())
		);
	}
}

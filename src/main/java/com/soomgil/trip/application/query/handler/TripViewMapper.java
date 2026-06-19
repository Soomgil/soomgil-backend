package com.soomgil.trip.application.query.handler;

import com.soomgil.trip.application.port.TripMemberReadModel;
import com.soomgil.trip.application.port.TripReadModel;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.dto.TripSummaryView;
import com.soomgil.trip.domain.model.TripAccessRole;
import java.util.UUID;

final class TripViewMapper {

	private TripViewMapper() {
	}

	static TripAccessRole accessRole(UUID ownerUserId, UUID userId) {
		return ownerUserId.equals(userId) ? TripAccessRole.OWNER : TripAccessRole.MEMBER;
	}

	static TripSummaryView toSummaryView(TripReadModel trip, UUID userId) {
		return new TripSummaryView(
			trip.id(),
			trip.title(),
			trip.displayDestination(),
			trip.status(),
			accessRole(trip.ownerUserId(), userId),
			trip.itineraryVersion(),
			trip.createdAt()
		);
	}

	static TripMemberView toMemberView(TripMemberReadModel member) {
		return new TripMemberView(
			member.id(),
			member.tripId(),
			member.userId(),
			member.role(),
			accessRole(member.ownerUserId(), member.userId()),
			member.status(),
			member.joinedAt()
		);
	}
}

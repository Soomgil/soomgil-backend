package com.soomgil.community.application.service;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.api.dto.ItineraryDay;
import com.soomgil.itinerary.api.dto.ItineraryDayGroupType;
import com.soomgil.itinerary.api.dto.ItineraryItem;
import com.soomgil.itinerary.api.dto.ItineraryItemType;
import com.soomgil.itinerary.api.dto.RouteSegment;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.RouteSegmentView;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryDayDetailView;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.user.api.dto.UserSummary;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** 여행방 권한과 itinerary version을 검증해 게시글 발행용 snapshot을 만든다. */
@Component
public class TripItinerarySnapshotChecker implements TripSnapshotChecker {

	private final TripAccessGuard accessGuard;
	private final FindItineraryHandler itineraryHandler;
	private final FindDisplayNameQueryHandler displayNameHandler;

	public TripItinerarySnapshotChecker(
		TripAccessGuard accessGuard,
		FindItineraryHandler itineraryHandler,
		FindDisplayNameQueryHandler displayNameHandler
	) {
		this.accessGuard = Objects.requireNonNull(accessGuard, "accessGuard must not be null");
		this.itineraryHandler = Objects.requireNonNull(itineraryHandler, "itineraryHandler must not be null");
		this.displayNameHandler = Objects.requireNonNull(displayNameHandler, "displayNameHandler must not be null");
	}

	@Override
	public CommunityPostSnapshot fetchSnapshot(UUID sourceTripId, long baseVersion, UUID publisherUserId) {
		accessGuard.requireActiveMember(sourceTripId, publisherUserId);
		ItineraryView itinerary = itineraryHandler.handle(new FindItineraryQuery(sourceTripId, publisherUserId));
		if (itinerary.itineraryVersion() == null || itinerary.itineraryVersion() != baseVersion) {
			throw new CommunityException(ErrorCode.SOURCE_TRIP_VERSION_CONFLICT);
		}
		FindDisplayNameQuery publisher = new FindDisplayNameQuery(publisherUserId);
		return new CommunityPostSnapshot(
			itinerary.days().stream().map(this::toDay).toList(),
			itinerary.routes().stream().map(this::toRoute).toList(),
			new UserSummary(
				publisherUserId,
				displayNameHandler.handle(publisher),
				displayNameHandler.findProfileImageUrl(publisher)
			)
		);
	}

	private ItineraryDay toDay(ItineraryDayDetailView day) {
		return new ItineraryDay(
			day.id(), day.tripId(), ItineraryDayGroupType.valueOf(day.groupType().name()), day.dayNumber(),
			day.date(), day.title(), day.sortOrder(), day.items().stream().map(this::toItem).toList()
		);
	}

	private ItineraryItem toItem(ItineraryItemView item) {
		PlaceRef place = item.placeProvider() == null || item.externalPlaceId() == null
			? null : new PlaceRef(PlaceProvider.valueOf(item.placeProvider()), item.externalPlaceId());
		return new ItineraryItem(
			item.id(), item.itineraryDayId(), item.sortOrder(), ItineraryItemType.valueOf(item.itemType().name()),
			place, item.placeName(), item.address(), item.lat(), item.lng(), item.thumbnailUrl(),
			PlaceSourceStatus.valueOf(item.sourceStatus())
		);
	}

	private RouteSegment toRoute(RouteSegmentView route) {
		return new RouteSegment(
			route.id(), route.originItineraryItemId(), route.destinationItineraryItemId(),
			com.soomgil.itinerary.api.dto.RouteMode.valueOf(route.mode().name()), route.provider(),
			route.providerProfile(), com.soomgil.itinerary.api.dto.GeometryFormat.valueOf(route.geometryFormat().name()),
			route.geometry(), route.distanceMeters(), route.durationSeconds(), route.confidence()
		);
	}
}

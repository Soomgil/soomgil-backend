package com.soomgil.community.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostRetripMapper;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.itinerary.application.port.ItineraryItemCreate;
import com.soomgil.itinerary.application.port.RouteSegmentCreate;
import com.soomgil.itinerary.domain.model.GeometryFormat;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.itinerary.domain.model.RouteMode;
import com.soomgil.trip.api.dto.TripAccessRole;
import com.soomgil.trip.api.dto.TripDetail;
import com.soomgil.trip.api.dto.TripMemberRole;
import com.soomgil.trip.api.dto.TripMemberStatus;
import com.soomgil.trip.api.dto.TripStatus;
import com.soomgil.trip.application.port.TripCommandRepository;
import com.soomgil.trip.domain.model.Trip;
import com.soomgil.trip.domain.model.TripMember;
import com.soomgil.user.api.dto.UserSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 저장된 게시글 snapshot을 독립된 새 여행방과 일정으로 복제한다. */
@Service
public class RetripCommunityPostService {

	private final CommunityPostMapper postMapper;
	private final PostRetripMapper retripMapper;
	private final TripCommandRepository tripRepository;
	private final ItineraryCommandRepository itineraryRepository;
	private final CommunityPostSnapshotCodec snapshotCodec;
	private final ObjectMapper objectMapper;
	private final FindDisplayNameQueryHandler displayNameHandler;

	public RetripCommunityPostService(
		CommunityPostMapper postMapper,
		PostRetripMapper retripMapper,
		TripCommandRepository tripRepository,
		ItineraryCommandRepository itineraryRepository,
		CommunityPostSnapshotCodec snapshotCodec,
		ObjectMapper objectMapper,
		FindDisplayNameQueryHandler displayNameHandler
	) {
		this.postMapper = Objects.requireNonNull(postMapper, "postMapper must not be null");
		this.retripMapper = Objects.requireNonNull(retripMapper, "retripMapper must not be null");
		this.tripRepository = Objects.requireNonNull(tripRepository, "tripRepository must not be null");
		this.itineraryRepository = Objects.requireNonNull(itineraryRepository, "itineraryRepository must not be null");
		this.snapshotCodec = Objects.requireNonNull(snapshotCodec, "snapshotCodec must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.displayNameHandler = Objects.requireNonNull(displayNameHandler, "displayNameHandler must not be null");
	}

	@Transactional
	public TripDetail retrip(UUID postId, UUID userId, String requestedTitle) {
		CommunityPostRecord post = postMapper.findById(postId)
			.orElseThrow(() -> new CommunityException(ErrorCode.POST_NOT_FOUND));
		if ((!post.isPubliclyVisible() && !post.isPublishedBy(userId)) || post.isDeleted()) {
			throw new CommunityException(ErrorCode.POST_NOT_FOUND);
		}
		CommunityPostSnapshot snapshot = snapshotCodec.decode(post.snapshotJson());
		Instant now = Instant.now();
		UUID tripId = UUID.randomUUID();
		UUID memberId = UUID.randomUUID();
		String title = requestedTitle == null || requestedTitle.isBlank() ? post.title() : requestedTitle;
		Trip trip = Trip.create(tripId, userId, title, null, now);
		TripMember owner = TripMember.initialOwnerMember(memberId, tripId, userId, now);
		tripRepository.saveCreatedRetrip(trip, owner, postId, post.snapshotVersion());

		boolean copied = copyItinerary(snapshot, tripId, userId, now);
		long itineraryVersion = 0;
		if (copied) {
			itineraryVersion = itineraryRepository.incrementItineraryVersion(tripId, 0, now)
				.orElseThrow(() -> new IllegalStateException("Retrip itinerary version could not be advanced."));
		}
		retripMapper.insert(UUID.randomUUID(), postId, userId, tripId, post.snapshotVersion(), now);
		FindDisplayNameQuery ownerQuery = new FindDisplayNameQuery(userId);

		return new TripDetail(
			tripId, trip.title(), null, TripStatus.ACTIVE, TripAccessRole.OWNER, itineraryVersion,
			OffsetDateTime.ofInstant(now, ZoneOffset.UTC), userId, List.of(),
			List.of(new com.soomgil.trip.api.dto.TripMember(
				memberId, tripId, new UserSummary(
					userId, displayNameHandler.handle(ownerQuery), displayNameHandler.findProfileImageUrl(ownerQuery)
				), TripMemberRole.MEMBER,
				TripAccessRole.OWNER, TripMemberStatus.ACTIVE, OffsetDateTime.ofInstant(now, ZoneOffset.UTC)
			)),
			postId
		);
	}

	private boolean copyItinerary(CommunityPostSnapshot snapshot, UUID tripId, UUID userId, Instant now) {
		Map<UUID, UUID> itemIds = new HashMap<>();
		for (var day : snapshot.days()) {
			UUID dayId = UUID.randomUUID();
			itineraryRepository.insertDay(new ItineraryDayCreate(
				dayId, tripId, ItineraryDayGroupType.valueOf(day.groupType().name()), day.dayNumber(),
				day.date(), day.title(), day.sortOrder(), now, now
			));
			for (var item : day.items()) {
				UUID itemId = UUID.randomUUID();
				itemIds.put(item.id(), itemId);
				itineraryRepository.insertItem(new ItineraryItemCreate(
					itemId, tripId, dayId, item.sortOrder(), ItineraryItemType.valueOf(item.itemType().name()),
					item.place() == null ? null : item.place().provider().name(),
					item.place() == null ? null : item.place().externalPlaceId(), item.placeName(), item.address(),
					item.lat(), item.lng(), item.thumbnailUrl() == null ? null : item.thumbnailUrl().toString(),
					item.sourceStatus().name(), userId, userId, now, now
				));
			}
		}
		for (var route : snapshot.routes()) {
			UUID originId = itemIds.get(route.originItineraryItemId());
			UUID destinationId = itemIds.get(route.destinationItineraryItemId());
			if (originId == null || destinationId == null) {
				throw new IllegalStateException("Retrip route references a missing snapshot item.");
			}
			itineraryRepository.insertRouteSegment(new RouteSegmentCreate(
				UUID.randomUUID(), tripId, originId, destinationId, RouteMode.valueOf(route.mode().name()),
				route.provider(), route.providerProfile() == null ? "" : route.providerProfile(),
				GeometryFormat.valueOf(route.geometryFormat().name()),
				json(route.geometry()), route.distanceMeters(), route.durationSeconds(), route.confidence(),
				userId, userId, now, now
			));
		}
		return !snapshot.days().isEmpty() || !snapshot.routes().isEmpty();
	}

	private String json(Map<String, Object> geometry) {
		try {
			return objectMapper.writeValueAsString(geometry);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalStateException("Retrip route geometry could not be serialized.", exception);
		}
	}
}

package com.soomgil.ai.application;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.query.dto.AccessibilityFlag;
import com.soomgil.place.application.query.dto.PlaceAccessibilityInfo;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.ListChecklistsQueryHandler;
import com.soomgil.planning.application.query.GetNoteQuery;
import com.soomgil.planning.application.query.ListChecklistsQuery;
import com.soomgil.record.api.dto.TripRecordEntry;
import com.soomgil.record.application.handler.TripRecordService;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 여행방의 현재 상태를 모델에 안전하게 전달할 compact context로 조립한다. */
@Service
public class AiTripContextService {

	private static final int RECENT_RECORD_LIMIT = 10;
	private static final int MAX_NOTE_LENGTH = 1_000;
	private static final int MAX_RECORD_CAPTION_LENGTH = 700;

	private final FindTripDetailHandler tripDetailHandler;
	private final FindItineraryHandler itineraryHandler;
	private final TripRecordService recordService;
	private final GetNoteQueryHandler noteHandler;
	private final ListChecklistsQueryHandler checklistHandler;
	private final FindDisplayNameQueryHandler displayNameHandler;
	private final PlaceAccessibilityCacheService accessibilityCacheService;

	public AiTripContextService(
		FindTripDetailHandler tripDetailHandler,
		FindItineraryHandler itineraryHandler,
		TripRecordService recordService,
		GetNoteQueryHandler noteHandler,
		ListChecklistsQueryHandler checklistHandler,
		FindDisplayNameQueryHandler displayNameHandler,
		PlaceAccessibilityCacheService accessibilityCacheService
	) {
		this.tripDetailHandler = tripDetailHandler;
		this.itineraryHandler = itineraryHandler;
		this.recordService = recordService;
		this.noteHandler = noteHandler;
		this.checklistHandler = checklistHandler;
		this.displayNameHandler = displayNameHandler;
		this.accessibilityCacheService = accessibilityCacheService;
	}

	/**
	 * 요청자가 접근할 수 있는 여행방 상태를 모델 입력용 공개 맥락으로 조립한다.
	 *
	 * <p>최근 기록은 최대 10개만 포함하고, 다른 멤버의 원시 취향 정보는 조회하지 않는다.
	 *
	 * @param tripId 조회할 여행방 ID
	 * @param requesterUserId active member 권한을 검증할 요청자 ID
	 * @return 현재 여행방의 compact AI context
	 */
	@Transactional(readOnly = true)
	public AiTripContext load(UUID tripId, UUID requesterUserId) {
		TripDetailView trip = tripDetailHandler.handle(new FindTripDetailQuery(tripId, requesterUserId));
		ItineraryView itinerary = itineraryHandler.handle(new FindItineraryQuery(tripId, requesterUserId));
		Map<UUID, String> names = memberNames(trip);
		List<TripRecordEntry> records = recordService.listRecords(
			tripId, requesterUserId, 0, RECENT_RECORD_LIMIT, List.of("takenAt,desc", "createdAt,desc")
		).items();
		List<Checklist> checklists = checklistHandler.handle(new ListChecklistsQuery(
			tripId, null, null, requesterUserId
		));
		Map<String, PlaceAccessibilityInfo> accessibilityByPlace = loadAccessibility(itinerary);

		return new AiTripContext(
			new AiTripContext.TripSummary(
				trip.id(), trip.title(), trip.displayDestination(), trip.status().name(),
				trip.myRole().name(), trip.itineraryVersion()
			),
			trip.members().stream().map(member -> new AiTripContext.MemberSummary(
				member.userId(), names.get(member.userId()), member.accessRole().name()
			)).toList(),
			itinerary.days().stream().map(day -> new AiTripContext.DaySummary(
				day.id(), day.groupType().name(), day.dayNumber(), day.date(), day.title(),
				day.items().stream().map(item -> new AiTripContext.ItemSummary(
					item.id(), item.sortOrder(), item.itemType().name(), item.placeProvider(),
					item.externalPlaceId(), item.placeName(), item.address(), item.lat(), item.lng(),
					accessibilitySummary(accessibilityByPlace, item)
				)).toList()
			)).toList(),
			itinerary.routes().stream().map(route -> new AiTripContext.RouteSummary(
				route.id(), route.originItineraryItemId(), route.destinationItineraryItemId(),
				route.mode().name(), route.distanceMeters(), route.durationSeconds()
			)).toList(),
			itinerary.mapDrawings().stream().map(drawing -> new AiTripContext.DrawingSummary(
				drawing.id(), drawing.itineraryDayId(), drawing.drawingType().name(),
				drawing.label(), drawing.sortOrder()
			)).toList(),
			records.stream().map(record -> recordSummary(record, names)).toList(),
			loadNotes(tripId, requesterUserId, itinerary),
			checklists.stream().map(this::checklistSummary).toList()
		);
	}

	private Map<String, PlaceAccessibilityInfo> loadAccessibility(ItineraryView itinerary) {
		List<PlaceAccessibilityCacheService.PlaceRef> refs = itinerary.days().stream()
			.flatMap(day -> day.items().stream())
			.filter(item -> item.placeProvider() != null && item.externalPlaceId() != null)
			.map(item -> new PlaceAccessibilityCacheService.PlaceRef(
				item.placeProvider(),
				item.externalPlaceId(),
				null
			))
			.distinct()
			.toList();
		return accessibilityCacheService.getMany(refs);
	}

	private AiTripContext.AccessibilitySummary accessibilitySummary(
		Map<String, PlaceAccessibilityInfo> accessibilityByPlace,
		ItineraryItemView item
	) {
		if (item.placeProvider() == null || item.externalPlaceId() == null) {
			return null;
		}
		PlaceAccessibilityInfo accessibility = accessibilityByPlace.get(item.placeProvider() + ":" + item.externalPlaceId());
		if (accessibility == null) {
			return null;
		}
		return new AiTripContext.AccessibilitySummary(
			accessibility.parkingType().name(),
			accessibility.flags().stream().map(AccessibilityFlag::name).sorted().toList(),
			accessibility.unavailableFlags().stream().map(AccessibilityFlag::name).sorted().toList()
		);
	}

	private Map<UUID, String> memberNames(TripDetailView trip) {
		Map<UUID, String> names = new LinkedHashMap<>();
		trip.members().forEach(member -> names.put(
			member.userId(), displayNameHandler.handle(new FindDisplayNameQuery(member.userId()))
		));
		return names;
	}

	private AiTripContext.RecordSummary recordSummary(TripRecordEntry record, Map<UUID, String> names) {
		UUID uploaderId = record.uploadedBy().id();
		String uploaderName = names.computeIfAbsent(
			uploaderId, id -> displayNameHandler.handle(new FindDisplayNameQuery(id))
		);
		return new AiTripContext.RecordSummary(
			record.id(), record.itineraryDayId(), record.itineraryItemId(), uploaderId, uploaderName,
			record.title(), truncate(record.caption(), MAX_RECORD_CAPTION_LENGTH), record.locationName(), record.takenAt()
		);
	}

	private List<AiTripContext.NoteSummary> loadNotes(UUID tripId, UUID userId, ItineraryView itinerary) {
		List<AiTripContext.NoteSummary> notes = new ArrayList<>();
		addNoteIfPresent(notes, tripId, userId, PlanningScopeType.TRIP, null);
		itinerary.days().forEach(day -> addNoteIfPresent(
			notes, tripId, userId, PlanningScopeType.DAY, day.id()
		));
		return List.copyOf(notes);
	}

	private void addNoteIfPresent(
		List<AiTripContext.NoteSummary> notes,
		UUID tripId,
		UUID userId,
		PlanningScopeType scope,
		UUID dayId
	) {
		noteHandler.findOptional(new GetNoteQuery(tripId, scope, dayId, userId))
			.ifPresent(note -> notes.add(new AiTripContext.NoteSummary(
				scope.name(), dayId, truncate(note.content(), MAX_NOTE_LENGTH)
			)));
	}

	private AiTripContext.ChecklistSummary checklistSummary(Checklist checklist) {
		return new AiTripContext.ChecklistSummary(
			checklist.id(), checklist.scopeType().name(), checklist.itineraryDayId(), checklist.title(),
			checklist.items().stream().map(item -> new AiTripContext.ChecklistItemSummary(
				item.id(), item.sortOrder(), item.content(),
				(int) item.memberStatuses().stream().filter(status -> status.isCompleted()).count(),
				item.memberStatuses().size()
			)).toList()
		);
	}

	private String truncate(String value, int maxLength) {
		if (value == null || value.length() <= maxLength) {
			return value;
		}
		return value.substring(0, maxLength) + "…";
	}
}

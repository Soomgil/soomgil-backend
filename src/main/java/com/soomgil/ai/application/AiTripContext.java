package com.soomgil.ai.application;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI 모델에 전달하는 여행방의 공개·업무 맥락이다.
 *
 * <p>다른 멤버의 원시 취향 점수와 태그는 포함하지 않는다. 모델 입력 크기를 제한하기 위해
 * 일정, 기록, planning 데이터도 답변에 필요한 필드만 보존한다.
 */
public record AiTripContext(
	TripSummary trip,
	List<MemberSummary> members,
	List<DaySummary> days,
	List<RouteSummary> routes,
	List<DrawingSummary> drawings,
	List<RecordSummary> recentRecords,
	List<NoteSummary> notes,
	List<ChecklistSummary> checklists
) {
	public AiTripContext {
		members = copy(members);
		days = copy(days);
		routes = copy(routes);
		drawings = copy(drawings);
		recentRecords = copy(recentRecords);
		notes = copy(notes);
		checklists = copy(checklists);
	}

	private static <T> List<T> copy(List<T> values) {
		return values == null ? List.of() : List.copyOf(values);
	}

	public record TripSummary(
		UUID id, String title, String destination, String status, String requesterRole, long itineraryVersion
	) {
	}

	public record MemberSummary(UUID userId, String displayName, String accessRole) {
	}

	public record DaySummary(
		UUID id, String groupType, Integer dayNumber, LocalDate date, String title, List<ItemSummary> items
	) {
		public DaySummary {
			items = copy(items);
		}
	}

	public record ItemSummary(
		UUID id, int sortOrder, String itemType, String placeProvider, String externalPlaceId,
		String placeName, String address, Double lat, Double lng
	) {
	}

	public record RouteSummary(
		UUID id, UUID originItemId, UUID destinationItemId, String mode,
		Double distanceMeters, Double durationSeconds
	) {
	}

	public record DrawingSummary(UUID id, UUID itineraryDayId, String drawingType, String label, int sortOrder) {
	}

	public record RecordSummary(
		UUID id, UUID itineraryDayId, UUID itineraryItemId, UUID uploadedByUserId,
		String uploadedByName, String title, String caption, String locationName, OffsetDateTime takenAt
	) {
	}

	public record NoteSummary(String scopeType, UUID itineraryDayId, String content) {
	}

	public record ChecklistSummary(
		UUID id, String scopeType, UUID itineraryDayId, String title, List<ChecklistItemSummary> items
	) {
		public ChecklistSummary {
			items = copy(items);
		}
	}

	public record ChecklistItemSummary(
		UUID id, int sortOrder, String content, int completedMemberCount, int memberStatusCount
	) {
	}
}

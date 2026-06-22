package com.soomgil.planning.application.service;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.ChecklistMemberStatus;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.ChecklistMemberStatusRecord;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.domain.model.NoteRecord;
import com.soomgil.user.api.dto.UserSummary;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * planning domain record를 API 응답 DTO로 조립한다.
 *
 * <p>member status의 {@link UserSummary}는 {@link FindDisplayNameQueryHandler}를 통해
	 * auth 모듈의 application interface로 해결한다.
 *
 * <p>{@link PlanningMutationResponse}의 {@code itineraryVersion}/{@code commandEventId}/
 * {@code undoAvailable}/{@code redoAvailable}은 collaboration/itinerary 모듈 연동 전까지 stub.
 * DBML planning 스키마에는 version 컬럼이 없으므로 resource 단위 version은 노출하지 않는다.
 */
@Component
public class PlanningAssembler {

	private final FindDisplayNameQueryHandler displayNameQueryHandler;

	public PlanningAssembler(FindDisplayNameQueryHandler displayNameQueryHandler) {
		this.displayNameQueryHandler = displayNameQueryHandler;
	}

	/**
	 * {@link NoteRecord}를 {@link Note}로 변환한다.
	 *
	 * @param record note row
	 * @return note DTO
	 */
	public Note toNoteDto(NoteRecord record) {
		return new Note(
			record.id(),
			record.tripId(),
			record.scopeType(),
			record.itineraryDayId(),
			record.content(),
			toOffsetDateTime(record.deletedAt())
		);
	}

	/**
	 * {@link ChecklistRecord}와 종속 item/statuses로 {@link Checklist}를 조립한다.
	 *
	 * @param record checklist row
	 * @param items checklist의 활성 item (sortOrder ASC)
	 * @param statusesByItemId itemId별 member status 목록
	 * @return checklist DTO
	 */
	public Checklist toChecklistDto(
		ChecklistRecord record,
		List<ChecklistItemRecord> items,
		Map<UUID, List<ChecklistMemberStatusRecord>> statusesByItemId
	) {
		List<ChecklistItem> itemDtos = items.stream()
			.map(item -> toItemDto(item, statusesByItemId.getOrDefault(item.id(), List.of())))
			.toList();
		return new Checklist(
			record.id(),
			record.tripId(),
			record.scopeType(),
			record.itineraryDayId(),
			record.title(),
			itemDtos
		);
	}

	/**
	 * {@link ChecklistItemRecord}와 member status 목록으로 {@link ChecklistItem}을 조립한다.
	 *
	 * @param record item row
	 * @param statuses item에 달린 member status
	 * @return item DTO
	 */
	public ChecklistItem toItemDto(
		ChecklistItemRecord record,
		List<ChecklistMemberStatusRecord> statuses
	) {
		List<ChecklistMemberStatus> statusDtos = statuses.stream()
			.map(this::toMemberStatusDto)
			.toList();
		return new ChecklistItem(
			record.id(),
			record.checklistId(),
			record.sortOrder(),
			record.content(),
			statusDtos,
			toOffsetDateTime(record.deletedAt())
		);
	}

	/**
	 * {@link ChecklistMemberStatusRecord}를 DTO로 변환한다.
	 * 사용자 display name은 {@link FindDisplayNameQueryHandler}로 해결한다.
	 *
	 * @param record member status row
	 * @return member status DTO
	 */
	public ChecklistMemberStatus toMemberStatusDto(ChecklistMemberStatusRecord record) {
		FindDisplayNameQuery query = new FindDisplayNameQuery(record.userId());
		return new ChecklistMemberStatus(
			new UserSummary(
				record.userId(),
				displayNameQueryHandler.handle(query),
				displayNameQueryHandler.findProfileImageUrl(query)
			),
			record.isCompleted(),
			toOffsetDateTime(record.completedAt()),
			toOffsetDateTime(record.updatedAt())
		);
	}

	/**
	 * note mutation 응답을 조립한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param note note DTO
	 * @return mutation 응답
	 */
	public PlanningMutationResponse toMutationResponse(UUID tripId, Note note) {
		return new PlanningMutationResponse(
			tripId, null, null, false, false, note, null, null, null);
	}

	/**
	 * checklist mutation 응답을 조립한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param checklist checklist DTO
	 * @return mutation 응답
	 */
	public PlanningMutationResponse toMutationResponse(UUID tripId, Checklist checklist) {
		return new PlanningMutationResponse(
			tripId, null, null, false, false, null, checklist, null, null);
	}

	/**
	 * checklist item mutation 응답을 조립한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param item item DTO
	 * @return mutation 응답
	 */
	public PlanningMutationResponse toMutationResponse(UUID tripId, ChecklistItem item) {
		return new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, item, null);
	}

	/**
	 * member status mutation 응답을 조립한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param status status DTO
	 * @return mutation 응답
	 */
	public PlanningMutationResponse toMutationResponse(
		UUID tripId, ChecklistMemberStatus status) {
		return new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, null, status);
	}

	private OffsetDateTime toOffsetDateTime(java.time.Instant instant) {
		return instant != null ? OffsetDateTime.ofInstant(instant, ZoneOffset.UTC) : null;
	}
}

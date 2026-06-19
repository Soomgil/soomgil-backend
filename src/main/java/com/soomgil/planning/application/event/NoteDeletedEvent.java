package com.soomgil.planning.application.event;

import java.util.UUID;

/**
 * note soft delete 이벤트.
 *
 * <p>클라이언트는 {@code noteId}로 로컬 상태에서 해당 note를 제거한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 삭제 수행자
 * @param noteId 삭제된 note 식별자
 * @param version tombstone version (삭제 후 증가한 값)
 */
public record NoteDeletedEvent(
	UUID tripId,
	UUID actorUserId,
	UUID noteId,
	long version
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.note.deleted";
	}
}

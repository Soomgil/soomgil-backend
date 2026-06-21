package com.soomgil.planning.application.event;

import com.soomgil.planning.api.dto.Note;
import java.util.UUID;

/**
 * note 생성 또는 갱신 이벤트.
 *
 * <p>{@code note} DTO에 mutation 결과(새 version 포함)를 담아 클라이언트가 추가 fetch 없이
 * 화면을 갱신할 수 있게 한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 변경 수행자
 * @param note mutation 결과 note DTO
 */
public record NoteUpsertedEvent(
	UUID tripId,
	UUID actorUserId,
	Note note
) implements PlanningRealtimeEvent {

	@Override
	public String eventType() {
		return "planning.note.upserted";
	}
}

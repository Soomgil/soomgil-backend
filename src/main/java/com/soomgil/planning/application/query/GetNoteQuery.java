package com.soomgil.planning.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningScopeType;
import java.util.UUID;

/**
 * (trip, scope, day) 조합으로 note를 조회한다.
 *
 * @param tripId 여행방 식별자
 * @param scopeType 범위
 * @param itineraryDayId 일차 식별자 (TRIP scope이면 null)
 * @param viewerUserId 조회자 (권한 검증용)
 */
public record GetNoteQuery(
	UUID tripId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	UUID viewerUserId
) implements Query<Note> {
}

package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import java.util.UUID;

/**
 * note upsert(생성 또는 갱신) 요청.
 *
 * <p>{@code (tripId, scopeType, itineraryDayId)} 조합으로 활성 note가 이미 존재하면
 * UPDATE하고, 없으면 새로 INSERT한다. DBML에 version 컬럼이 없으므로 optimistic lock은
 * 리소스 단위가 아닌 상위 itinerary_version으로 관리된다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 요청자
 * @param scopeType 범위 (TRIP 또는 DAY)
 * @param itineraryDayId DAY scope인 경우 일차 식별자. TRIP scope이면 null
 * @param content 본문
 */
public record UpsertNoteCommand(
	UUID tripId,
	UUID actorUserId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String content
) implements Command<PlanningMutationResponse> {
}

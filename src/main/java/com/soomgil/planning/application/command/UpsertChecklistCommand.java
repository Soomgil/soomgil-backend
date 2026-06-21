package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import java.util.UUID;

/**
 * checklist upsert(생성 또는 title 갱신) 요청.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 요청자
 * @param scopeType 범위
 * @param itineraryDayId DAY scope인 경우 일차 식별자
 * @param title 표시용 제목 (nullable)
 */
public record UpsertChecklistCommand(
	UUID tripId,
	UUID actorUserId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String title
) implements Command<PlanningMutationResponse> {
}

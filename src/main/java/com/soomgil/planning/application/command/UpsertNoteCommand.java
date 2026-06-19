package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import java.util.UUID;

/**
 * note upsert(생성 또는 갱신) 요청.
 *
 * <p>{@code (tripId, scopeType, itineraryDayId)} 조합으로 활성 note가 이미 존재하면
 * version을 검증해 UPDATE하고, 없으면 새로 INSERT한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 요청자
 * @param baseVersion 기존 note의 version. 신규 INSERT 시 값은 무시됨
 * @param scopeType 범위 (TRIP 또는 DAY)
 * @param itineraryDayId DAY scope인 경우 일차 식별자. TRIP scope이면 null
 * @param content 본문
 */
public record UpsertNoteCommand(
	UUID tripId,
	UUID actorUserId,
	long baseVersion,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String content
) implements Command<PlanningMutationResponse> {
}

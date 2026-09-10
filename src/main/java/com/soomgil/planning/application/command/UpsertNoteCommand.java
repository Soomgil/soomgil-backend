package com.soomgil.planning.application.command;

import com.soomgil.common.cqrs.Command;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import java.util.UUID;

/**
 * note upsert(생성 또는 갱신) 요청.
 *
 * <p>{@code (tripId, scopeType, itineraryDayId)} 조합으로 활성 note가 이미 존재하면
 * UPDATE하고, 없으면 새로 INSERT한다. {@code baseVersion}이 현재 메모 버전과 다르면
 * 다른 참여자의 변경을 덮어쓰지 않도록 충돌로 거절한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 요청자
 * @param scopeType 범위 (TRIP 또는 DAY)
 * @param itineraryDayId DAY scope인 경우 일차 식별자. TRIP scope이면 null
 * @param content 본문
 * @param baseVersion 클라이언트가 마지막으로 읽은 메모 버전. 신규 메모는 0
 */
public record UpsertNoteCommand(
	UUID tripId,
	UUID actorUserId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	String content,
	long baseVersion
) implements Command<PlanningMutationResponse> {
}

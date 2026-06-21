package com.soomgil.planning.application.query;

import com.soomgil.common.cqrs.Query;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.PlanningScopeType;
import java.util.List;
import java.util.UUID;

/**
 * trip의 checklist 목록을 조회한다.
 *
 * <p>{@code scopeType}/{@code itineraryDayId} 필터는 optional. 둘 다 null이면 trip의
 * 모든 활성 checklist를 반환한다. 각 checklist는 items와 member statuses를 함께 조립한다.
 *
 * @param tripId 여행방 식별자
 * @param scopeType scope 필터 (nullable)
 * @param itineraryDayId 일차 필터 (nullable)
 * @param viewerUserId 조회자 (권한 검증용)
 */
public record ListChecklistsQuery(
	UUID tripId,
	PlanningScopeType scopeType,
	UUID itineraryDayId,
	UUID viewerUserId
) implements Query<List<Checklist>> {
}

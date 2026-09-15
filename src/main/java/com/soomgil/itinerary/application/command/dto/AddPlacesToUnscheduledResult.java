package com.soomgil.itinerary.application.command.dto;

import java.util.List;
import java.util.UUID;

/**
 * 일차 미정 일괄 추가 결과.
 *
 * <p>{@code itineraryVersion}은 처리 후 여행방의 협업 version이다. 실제로 추가된 장소가 하나도 없으면
 * version을 올리지 않으므로 요청 전 version과 같을 수 있다.
 *
 * @param tripId 여행방 식별자
 * @param itineraryVersion 처리 후 itinerary version
 * @param unscheduledDayId 사용한 일차 미정 그룹 식별자. 추가할 것이 없어 그룹을 만들지 않았으면 null
 * @param added 새로 추가된 장소 목록
 * @param skippedDuplicates 이미 일정에 있어 건너뛴 장소 목록
 */
public record AddPlacesToUnscheduledResult(
	UUID tripId,
	long itineraryVersion,
	UUID unscheduledDayId,
	List<AddedUnscheduledPlace> added,
	List<SkippedUnscheduledPlace> skippedDuplicates
) {

	public AddPlacesToUnscheduledResult {
		added = added == null ? List.of() : List.copyOf(added);
		skippedDuplicates = skippedDuplicates == null ? List.of() : List.copyOf(skippedDuplicates);
	}
}

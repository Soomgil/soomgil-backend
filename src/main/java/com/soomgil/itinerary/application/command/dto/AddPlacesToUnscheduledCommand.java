package com.soomgil.itinerary.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.List;
import java.util.UUID;

/**
 * 선정된 장소들을 여행방의 일차 미정 그룹에 한 번에 추가하는 command.
 *
 * <p>다른 모듈이 itinerary mapper나 DB를 직접 건드리지 않고 일정에 장소를 넣을 때 사용하는 공개 계약이다.
 *
 * <p>사용자 편집이 아니라 서버가 시작하는 batch write이므로 호출자는 {@code baseVersion}을 전달하지 않는다.
 * handler가 현재 version을 읽어 batch 전체에 대해 한 번만 증가시킨다.
 *
 * <p>같은 {@code placeProvider + externalPlaceId}가 이미 여행방의 active 일정 item으로 존재하면
 * 실제 day 소속인지 일차 미정 소속인지와 관계없이 추가하지 않고 결과의 skipped 목록으로 보고한다.
 * 따라서 같은 command를 재시도해도 일정에 중복이 생기지 않는다.
 *
 * @param tripId 대상 여행방 식별자
 * @param actorUserId 이 변경의 주체가 되는 사용자. active member여야 한다
 * @param places 추가할 장소 목록. 빈 목록이면 아무 것도 하지 않는다
 * @param sourceReference 감사용 출처 문자열. 예: 투표 세션 식별자
 */
public record AddPlacesToUnscheduledCommand(
	UUID tripId,
	UUID actorUserId,
	List<UnscheduledPlaceToAdd> places,
	String sourceReference
) implements Command<AddPlacesToUnscheduledResult> {

	public AddPlacesToUnscheduledCommand {
		places = places == null ? List.of() : List.copyOf(places);
	}
}

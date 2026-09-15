package com.soomgil.itinerary.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledCommand;
import com.soomgil.itinerary.application.command.dto.AddPlacesToUnscheduledResult;

/**
 * 일차 미정 일괄 추가 command를 처리하는 application 계약.
 *
 * <p>투표 결과 반영처럼 다른 모듈이 일정에 장소를 넣어야 할 때 호출하는 공개 interface다.
 * 호출 모듈은 이 interface만 의존하고 itinerary의 repository, mapper, DB에는 접근하지 않는다.
 */
public interface AddPlacesToUnscheduledHandler
	extends CommandHandler<AddPlacesToUnscheduledCommand, AddPlacesToUnscheduledResult> {
}

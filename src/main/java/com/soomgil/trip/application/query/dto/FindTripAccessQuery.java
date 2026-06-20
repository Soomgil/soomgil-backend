package com.soomgil.trip.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.UUID;

/**
 * 특정 사용자의 여행방 접근 가능 여부를 조회하는 query.
 *
 * <p>다른 모듈은 trip DB를 직접 읽지 않고 이 query handler를 호출해 active member와
 * owner 권한을 확인한다.
 */
public record FindTripAccessQuery(
	UUID tripId,
	UUID userId
) implements Query<TripAccessView> {
}

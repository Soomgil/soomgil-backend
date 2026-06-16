package com.soomgil.trip.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.UUID;

/**
 * 여행방 상세 화면에 필요한 읽기 모델을 조회하는 query.
 *
 * <p>handler는 요청 사용자의 active member 권한을 먼저 확인한 뒤 상세 정보를 반환한다.
 */
public record FindTripDetailQuery(
	UUID tripId,
	UUID userId
) implements Query<TripDetailView> {
}

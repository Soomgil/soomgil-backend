package com.soomgil.trip.application.query.dto;

import com.soomgil.common.cqrs.Query;
import java.util.List;
import java.util.UUID;

/**
 * 여행방의 법정동 코드 목록을 조회하는 query.
 *
 * <p>다른 모듈은 trip DB를 직접 읽지 않고 이 query를 통해 여행 지역을 확인한다.
 * handler는 요청 사용자의 active member 권한을 먼저 확인한다.
 *
 * @param tripId 여행방 ID
 * @param userId 요청 사용자 ID
 */
public record ListTripRegionCodesQuery(
	UUID tripId,
	UUID userId
) implements Query<List<String>> {
}

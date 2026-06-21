package com.soomgil.place.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.place.api.dto.PlaceDetail;
import com.soomgil.place.api.dto.PlaceProvider;

/**
 * provider와 외부 장소 id로 관광지 상세를 조회하는 query.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 */
public record PlaceDetailQuery(
	PlaceProvider provider,
	String externalPlaceId
) implements Query<PlaceDetail> {
}

package com.soomgil.social.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.place.api.dto.PlaceRef;
import java.util.List;

/**
 * 현재 사용자가 팔로우하는 사용자의 장소별 긍정 반응을 조회하는 query.
 *
 * <p>{@code LIKE}, {@code SUPER_LIKE}만 조회하며 다른 사용자의 취향 점수나 원본 스와이프 로그는 반환하지 않는다.
 *
 * @param places 반응을 조회할 장소 목록
 */
public record FindFolloweePlaceReactionsQuery(
	List<PlaceRef> places
) implements Query<List<FolloweePlaceReaction>> {

	public FindFolloweePlaceReactionsQuery {
		places = places == null ? List.of() : List.copyOf(places);
	}
}

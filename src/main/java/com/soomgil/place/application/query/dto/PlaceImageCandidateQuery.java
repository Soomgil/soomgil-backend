package com.soomgil.place.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.place.api.dto.PlaceProvider;
import java.util.List;

/**
 * provider와 외부 장소 id로 장소 이미지 후보를 조회하는 query.
 *
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 */
public record PlaceImageCandidateQuery(
	PlaceProvider provider,
	String externalPlaceId
) implements Query<List<PlaceImageCandidate>> {
}

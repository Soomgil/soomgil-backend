package com.soomgil.voting.api.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.UUID;

/**
 * 투표 후보 관광지 응답.
 *
 * <p>투표 시작 시점에 고정된 snapshot이며 투표 도중 추천 결과가 바뀌어도 변하지 않는다.
 * 다른 참여자의 취향 점수나 태그는 포함하지 않는다.
 *
 * <p>{@code stickerCount}는 투표가 진행 중인 동안에는 노출하지 않기 위해 null이며,
 * 종료된 세션의 결과 조회에서만 채워진다.
 *
 * @param id 후보 식별자. 스티커를 붙일 때 이 값을 사용한다
 * @param rank snapshot 순서
 * @param provider 장소 원천 provider
 * @param externalPlaceId 외부 장소 id
 * @param name 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 대표 이미지 URL
 * @param category 관광지 분류
 * @param stickerCount 종료 후 집계된 스티커 총합. 진행 중이면 null
 */
public record TripVoteCandidate(
	@NotNull
	UUID id,
	int rank,
	PlaceProvider provider,
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String category,
	Integer stickerCount
) {
}

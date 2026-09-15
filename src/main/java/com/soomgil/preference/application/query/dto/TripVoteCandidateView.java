package com.soomgil.preference.application.query.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import java.net.URI;

/**
 * 투표 후보로 사용할 장소.
 *
 * <p>이 view에는 다른 참여자의 raw/normalized 선호도 점수, 세부 태그 가중치, 매칭 멤버 정보를 담지 않는다.
 * 여행 방 안에서 다른 멤버의 취향을 공개하지 않는다는 정책을 계약 수준에서 강제하기 위해
 * 노출 필드를 장소 표시 정보와 순위로만 한정한다.
 *
 * @param rank 1부터 시작하는 추천 순위
 * @param provider 장소 원천 provider
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param name 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 대표 이미지 URL
 * @param category 관광지 분류
 */
public record TripVoteCandidateView(
	int rank,
	PlaceProvider provider,
	String externalPlaceId,
	String name,
	String address,
	Double lat,
	Double lng,
	URI thumbnailUrl,
	String category
) {
}

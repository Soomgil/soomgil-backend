package com.soomgil.voting.application.port;

import java.util.UUID;

/**
 * 투표 후보 snapshot record.
 *
 * <p>투표 시작 시점의 추천 결과를 고정한 값이다. 투표 도중 추천 결과가 바뀌어도 이 row는 변하지 않는다.
 * {@code stickerCount}, {@code selected}, {@code selectedRank}만 투표 진행/종료에 따라 갱신된다.
 *
 * @param id 후보 식별자
 * @param voteSessionId 세션 식별자
 * @param sortOrder snapshot 순서. 1부터 시작한다
 * @param placeProvider 장소 원천 provider
 * @param externalPlaceId 외부 장소 id
 * @param placeName 장소명
 * @param address 주소
 * @param lat 위도
 * @param lng 경도
 * @param thumbnailUrl 대표 이미지 URL
 * @param category 관광지 분류
 * @param stickerCount 모든 참여자가 붙인 스티커 총합
 * @param selected 최종 선정 여부
 * @param selectedRank 선정 순위. 선정되지 않았으면 null
 */
public record VoteCandidateRecord(
	UUID id,
	UUID voteSessionId,
	int sortOrder,
	String placeProvider,
	String externalPlaceId,
	String placeName,
	String address,
	Double lat,
	Double lng,
	String thumbnailUrl,
	String category,
	int stickerCount,
	boolean selected,
	Integer selectedRank
) {
}

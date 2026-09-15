package com.soomgil.preference.application.command.dto;

/**
 * 투표에서 한 사용자가 특정 장소에 붙인 스티커 정보.
 *
 * <p>{@code stickerCount}는 사용자가 그 장소에 몰아붙인 개수 그대로이며 취향 근거 가중치 계산의 입력이다.
 *
 * @param provider 장소 원천 provider. 예: {@code KTO}
 * @param externalPlaceId provider가 부여한 외부 장소 id
 * @param stickerCount 해당 장소에 붙인 스티커 개수. 0 이하이면 반영 대상이 아니다
 */
public record TripVoteStickerPlace(
	String provider,
	String externalPlaceId,
	int stickerCount
) {
}

package com.soomgil.voting.api.dto;

import com.soomgil.place.api.dto.PlaceProvider;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 투표 종료 후 후보별 결과.
 *
 * <p>{@code selectedRank}가 같은 항목이 여러 개면 마지막 순위 동점으로 함께 선정된 것이다.
 * {@code itineraryOutcome}이 {@code SKIPPED_DUPLICATE}이면 이미 일정에 있어 추가하지 않은 경우다.
 *
 * @param candidateId 후보 식별자
 * @param provider 장소 원천 provider
 * @param externalPlaceId 외부 장소 id
 * @param name 장소명
 * @param thumbnailUrl 대표 이미지 URL
 * @param stickerCount 스티커 총합
 * @param selected 최종 선정 여부
 * @param selectedRank 선정 순위. 선정되지 않았으면 null
 * @param itineraryOutcome 일정 반영 결과. 선정되지 않았으면 null
 * @param itineraryItemId 생성된 일정 item 식별자. 추가되지 않았으면 null
 */
public record TripVoteResultItem(
	@NotNull
	UUID candidateId,
	PlaceProvider provider,
	String externalPlaceId,
	String name,
	java.net.URI thumbnailUrl,
	int stickerCount,
	boolean selected,
	Integer selectedRank,
	String itineraryOutcome,
	UUID itineraryItemId
) {
}

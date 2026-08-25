package com.soomgil.preference.infrastructure.persistence.row;

import java.math.BigDecimal;

/**
 * TRIP_VOTE 취향 근거 감사 row.
 *
 * <p>{@code (voteSessionId, userId, provider, externalPlaceId)} unique 제약이 멱등성의 근거다.
 *
 * @param id 근거 row 식별자
 * @param voteSessionId 투표 세션 식별자
 * @param userId 사용자 식별자
 * @param provider 장소 원천 provider
 * @param externalPlaceId 외부 장소 id
 * @param stickerCount 사용자가 해당 장소에 붙인 스티커 개수
 * @param source 근거 출처. 항상 {@code TRIP_VOTE}
 * @param placeTagEnrichmentId 근거 계산에 사용한 태그 enrichment 식별자. 확정 태그가 없으면 null
 * @param evidenceUnits 정책이 환산한 근거 단위
 * @param calculationVersion 환산에 사용한 정책 버전
 */
public record UserPlaceVoteEvidenceInsertRow(
	String id,
	String voteSessionId,
	String userId,
	String provider,
	String externalPlaceId,
	int stickerCount,
	String source,
	String placeTagEnrichmentId,
	BigDecimal evidenceUnits,
	String calculationVersion
) {
}

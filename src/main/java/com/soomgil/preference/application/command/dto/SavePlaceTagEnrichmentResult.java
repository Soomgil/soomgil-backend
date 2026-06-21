package com.soomgil.preference.application.command.dto;

import java.util.UUID;

/**
 * 장소 태깅 실행 결과 저장 처리 결과.
 *
 * @param enrichmentId 생성한 enrichment id
 * @param candidateCount 저장한 후보 수
 * @param selectedCount 확정 태그로 저장한 수
 */
public record SavePlaceTagEnrichmentResult(
	UUID enrichmentId,
	int candidateCount,
	int selectedCount
) {
}

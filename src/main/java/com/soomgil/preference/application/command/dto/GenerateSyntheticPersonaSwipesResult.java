package com.soomgil.preference.application.command.dto;

/**
 * 합성 페르소나 스와이프 생성 결과.
 *
 * @param generatorVersion 사용한 고정 generator version
 * @param personaCount 검증하고 저장한 페르소나 수
 * @param placeCount 처리한 장소 수
 * @param eventCount 생성 또는 갱신한 결정적 이벤트 수
 */
public record GenerateSyntheticPersonaSwipesResult(
	String generatorVersion,
	int personaCount,
	int placeCount,
	int eventCount
) {
}

package com.soomgil.preference.application.command.dto;

/**
 * 투표 스티커 취향 반영 결과.
 *
 * @param appliedPlaceCount 이번 호출에서 실제로 근거가 반영된 장소 수
 * @param skippedAlreadyAppliedCount 이미 반영되어 있어 건너뛴 장소 수. 재시도 시 여기로 집계된다
 */
public record ApplyTripVotePreferenceResult(
	int appliedPlaceCount,
	int skippedAlreadyAppliedCount
) {
}

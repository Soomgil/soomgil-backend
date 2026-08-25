package com.soomgil.voting.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 투표 시작 요청. 방장만 호출할 수 있다.
 *
 * <p>{@code stickerAllowance}와 {@code selectionCount}는 투표 시작 후 변경할 수 없고,
 * 둘 다 실제로 생성된 후보 관광지 수를 넘을 수 없다.
 *
 * @param stickerAllowance 사용자당 스티커 지급 개수
 * @param selectionCount 최종 선정 관광지 개수
 * @param candidateCount 만들 후보 수. 생략하면 기본 10개
 */
public record OpenVoteSessionRequest(
	@Min(1)
	@Max(100)
	int stickerAllowance,
	@Min(1)
	@Max(100)
	int selectionCount,
	@Min(1)
	@Max(100)
	Integer candidateCount
) {
}

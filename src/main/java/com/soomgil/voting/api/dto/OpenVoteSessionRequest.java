package com.soomgil.voting.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Objects;

/**
 * 투표 시작 요청. 방장만 호출할 수 있다.
 *
 * <p>{@code stickerAllowance}와 {@code selectionCount}는 투표 시작 후 변경할 수 없고,
 * 둘 다 실제로 생성된 후보 관광지 수를 넘을 수 없다.
 *
 * <p>{@code legalRegionCodes}를 주면 이번 투표의 후보를 그 지역에서만 뽑는다. 비우면 여행방에 등록된
 * 지역을 쓰고, 그것도 없으면 대표 목적지 문자열로 대체 검색한다. 셋 다 없으면 시작을 거절한다.
 *
 * @param stickerAllowance 사용자당 스티커 지급 개수
 * @param selectionCount 최종 선정 관광지 개수
 * @param candidateCount 만들 후보 수. 생략하면 기본 10개
 * @param legalRegionCodes 이번 투표에서 후보를 뽑을 10자리 법정동 코드. 생략하면 여행방 지역을 쓴다
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
	Integer candidateCount,
	@Size(max = 20)
	List<@Size(min = 10, max = 10) String> legalRegionCodes
) {
	public OpenVoteSessionRequest {
		legalRegionCodes = legalRegionCodes == null
			? List.of()
			: legalRegionCodes.stream().filter(Objects::nonNull).distinct().toList();
	}

	/**
	 * 지역을 고르지 않은 요청을 만든다.
	 */
	public OpenVoteSessionRequest(int stickerAllowance, int selectionCount, Integer candidateCount) {
		this(stickerAllowance, selectionCount, candidateCount, List.of());
	}
}

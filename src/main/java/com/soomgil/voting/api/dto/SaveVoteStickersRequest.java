package com.soomgil.voting.api.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 스티커 배치 저장 요청.
 *
 * <p>부분 변경이 아니라 현재 배치 전체를 보내는 snapshot 방식이다. 목록에서 빠진 후보는 스티커가
 * 회수된 것으로 처리되므로 이동과 회수를 별도 API 없이 표현할 수 있다.
 *
 * @param placements 현재 붙여 둔 스티커 배치 전체
 */
public record SaveVoteStickersRequest(
	@Valid
	List<VoteStickerPlacement> placements
) {
}

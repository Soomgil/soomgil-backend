package com.soomgil.voting.api.dto;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 투표 제출 요청.
 *
 * <p>{@code placements}를 함께 보내면 저장 후 제출하고, 생략하면 이미 저장된 배치로 제출한다.
 * 제출 후에는 수정할 수 없다.
 *
 * @param placements 제출과 함께 저장할 배치. 생략 가능
 */
public record SubmitVoteRequest(
	@Valid
	List<VoteStickerPlacement> placements
) {
}

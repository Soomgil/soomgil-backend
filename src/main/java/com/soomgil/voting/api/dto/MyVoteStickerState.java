package com.soomgil.voting.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 스티커 저장 결과 응답.
 *
 * @param sessionId 세션 식별자
 * @param myParticipation 저장 후 참여 상태
 */
public record MyVoteStickerState(
	@NotNull
	java.util.UUID sessionId,
	@Valid
	@NotNull
	MyVoteParticipation myParticipation
) {
}

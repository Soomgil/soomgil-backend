package com.soomgil.voting.application.command.dto;

import java.util.UUID;

/**
 * 후보 하나에 붙일 스티커 개수.
 *
 * @param candidateId 후보 식별자. 같은 세션의 후보여야 한다
 * @param stickerCount 붙일 개수. 1 이상
 */
public record VoteStickerPlacementCommand(
	UUID candidateId,
	int stickerCount
) {
}

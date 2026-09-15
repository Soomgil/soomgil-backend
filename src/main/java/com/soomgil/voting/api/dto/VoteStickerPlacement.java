package com.soomgil.voting.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * 후보 하나에 붙인 스티커 배치.
 *
 * <p>같은 관광지에 여러 개를 몰아붙일 수 있으므로 {@code stickerCount}는 1보다 클 수 있다.
 * 붙이지 않은 후보는 목록에 넣지 않는다.
 *
 * @param candidateId 후보 식별자
 * @param stickerCount 붙인 스티커 개수. 1 이상
 */
public record VoteStickerPlacement(
	@NotNull
	UUID candidateId,
	@Min(1)
	int stickerCount
) {
}

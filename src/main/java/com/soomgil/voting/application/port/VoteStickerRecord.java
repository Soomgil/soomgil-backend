package com.soomgil.voting.application.port;

import java.util.UUID;

/**
 * 참여자가 후보에 붙인 스티커 record.
 *
 * <p>같은 관광지에 여러 개를 몰아붙일 수 있으므로 row를 여러 개 만들지 않고 개수를 저장한다.
 * 이 개수는 결과 집계와 개인 취향 반영에 그대로 사용된다.
 *
 * @param id 스티커 record 식별자
 * @param voteSessionId 세션 식별자
 * @param participantId 참여자 식별자
 * @param candidateId 후보 식별자
 * @param stickerCount 해당 후보에 붙인 개수. 1 이상
 */
public record VoteStickerRecord(
	UUID id,
	UUID voteSessionId,
	UUID participantId,
	UUID candidateId,
	int stickerCount
) {
}

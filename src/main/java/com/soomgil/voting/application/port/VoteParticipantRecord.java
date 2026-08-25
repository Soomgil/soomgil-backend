package com.soomgil.voting.application.port;

import com.soomgil.voting.domain.model.VoteParticipantStatus;
import java.time.Instant;
import java.util.UUID;

/**
 * 투표 참여자 record.
 *
 * <p>투표 시작 시점의 활성 여행방 멤버로 확정되며, 이후 합류한 멤버는 추가되지 않는다.
 *
 * @param id 참여자 식별자
 * @param voteSessionId 세션 식별자
 * @param userId 사용자 식별자
 * @param status 참여 상태
 * @param stickerAllowance 이 참여자에게 지급된 스티커 개수
 * @param usedStickerCount 현재까지 사용한 스티커 총합
 * @param submittedAt 제출 시각. 제출 전이면 null
 */
public record VoteParticipantRecord(
	UUID id,
	UUID voteSessionId,
	UUID userId,
	VoteParticipantStatus status,
	int stickerAllowance,
	int usedStickerCount,
	Instant submittedAt
) {
}

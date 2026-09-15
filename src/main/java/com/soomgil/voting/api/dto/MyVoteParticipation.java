package com.soomgil.voting.api.dto;

import com.soomgil.voting.domain.model.VoteParticipantStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 현재 사용자의 투표 참여 상태.
 *
 * <p>첫 진입 여부를 boolean 하나로 두지 않고 세션별 참여 상태로 표현한다.
 * {@code status}가 {@code SUBMITTED}이면 더 이상 수정할 수 없다.
 *
 * @param participantId 참여자 식별자
 * @param status 참여 상태
 * @param stickerAllowance 지급받은 스티커 개수
 * @param usedStickerCount 사용한 스티커 총합
 * @param remainingStickerCount 남은 스티커 개수
 * @param placements 현재 붙여 둔 스티커 배치
 * @param submittedAt 제출 시각. 제출 전이면 null
 */
public record MyVoteParticipation(
	@NotNull
	UUID participantId,
	@NotNull
	VoteParticipantStatus status,
	int stickerAllowance,
	int usedStickerCount,
	int remainingStickerCount,
	@Valid
	@NotNull
	List<VoteStickerPlacement> placements,
	OffsetDateTime submittedAt
) {

	public MyVoteParticipation {
		placements = placements == null ? List.of() : List.copyOf(placements);
	}
}

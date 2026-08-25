package com.soomgil.voting.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.voting.api.dto.TripVoteSessionDetail;
import java.util.UUID;

/**
 * 투표 시작 command. 여행방 방장만 실행할 수 있다.
 *
 * <p>handler는 시작 시점의 활성 참여자와 추천 후보를 세션 snapshot으로 고정한다.
 * 이미 진행 중인 세션이 있으면 {@code VOTE_SESSION_ALREADY_OPEN}으로 거절한다.
 *
 * @param tripId 여행방 식별자
 * @param actorUserId 요청 사용자. 방장이어야 한다
 * @param stickerAllowance 사용자당 스티커 지급 개수
 * @param selectionCount 최종 선정 관광지 개수
 * @param candidateCount 만들 후보 수. null이면 기본 10개
 */
public record OpenVoteSessionCommand(
	UUID tripId,
	UUID actorUserId,
	int stickerAllowance,
	int selectionCount,
	Integer candidateCount
) implements Command<TripVoteSessionDetail> {
}

package com.soomgil.voting.api.dto;

import com.soomgil.voting.domain.model.VoteNextScreen;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/**
 * 여행 방 진입 시 프론트 라우터 가드가 사용하는 단일 응답.
 *
 * <p>클라이언트는 {@code nextScreen} 하나만 보고 분기하면 된다. 세션 상태와 참여 상태를 조합해
 * 화면을 정하는 책임은 서버에 둔다.
 *
 * @param hasSession 여행방에 참고할 투표 세션이 있는지 여부
 * @param nextScreen 먼저 보여줄 화면
 * @param session 세션 상세. 세션이 없으면 null
 * @param myParticipation 현재 사용자의 참여 상태. 참여자가 아니면 null
 */
public record TripVoteSessionState(
	boolean hasSession,
	@NotNull
	VoteNextScreen nextScreen,
	@Valid
	TripVoteSessionDetail session,
	@Valid
	MyVoteParticipation myParticipation
) {
}

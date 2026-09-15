package com.soomgil.voting.domain.model;

/**
 * 투표 세션별 참여자 상태.
 *
 * <p>첫 진입 여부를 단순 boolean 하나로 두지 않고 세션별 상태로 관리해, 여러 번의 투표와
 * 재진입을 정확히 구분한다.
 *
 * <p>{@code NOT_STARTED}는 아직 스티커를 붙이지 않은 상태, {@code IN_PROGRESS}는 스티커를 붙였지만
 * 제출하지 않은 상태, {@code SUBMITTED}는 제출을 마쳐 더 이상 수정할 수 없는 상태다.
 */
public enum VoteParticipantStatus {
	NOT_STARTED,
	IN_PROGRESS,
	SUBMITTED
}

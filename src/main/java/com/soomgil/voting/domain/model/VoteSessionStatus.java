package com.soomgil.voting.domain.model;

/**
 * 투표 세션 상태.
 *
 * <p>{@code DRAFT}는 방장이 설정만 해 두고 아직 열지 않은 상태를 위한 예약 값이다.
 * V1 API로는 생성되지 않으며 모든 세션은 {@code OPEN}으로 시작한다.
 *
 * <p>{@code COMPLETED}는 종료 상태다. 전원 제출 자동 종료와 방장 조기 종료 모두 이 상태로 끝나며,
 * 종료 이후에는 다시 열 수 없다.
 */
public enum VoteSessionStatus {
	DRAFT,
	OPEN,
	COMPLETED
}

package com.soomgil.voting.domain.model;

/**
 * 투표가 종료된 이유.
 *
 * <p>{@code ALL_SUBMITTED}는 마지막 참여자의 제출로 자동 종료된 경우,
 * {@code OWNER_EARLY_CLOSE}는 방장이 미투표자 경고를 확인하고 조기 종료한 경우다.
 */
public enum VoteCompletionReason {
	ALL_SUBMITTED,
	OWNER_EARLY_CLOSE
}

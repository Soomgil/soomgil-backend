package com.soomgil.voting.application.port;

import java.util.UUID;

/**
 * 투표 세션이 후보를 뽑은 지역 snapshot row.
 *
 * @param id 식별자
 * @param voteSessionId 세션 식별자
 * @param legalRegionCode 10자리 법정동 코드
 * @param regionName 시작 시점의 지역 이름
 * @param sortOrder 방장이 고른 순서
 */
public record VoteRegionRecord(
	UUID id,
	UUID voteSessionId,
	String legalRegionCode,
	String regionName,
	int sortOrder
) {
}

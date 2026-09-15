package com.soomgil.voting.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 투표 후보를 뽑은 지역. 투표 시작 시점에 이름까지 고정한 snapshot이다.
 *
 * @param code 10자리 법정동 코드
 * @param name 시작 시점의 지역 이름
 */
public record TripVoteRegion(
	@NotBlank
	String code,
	@NotBlank
	String name
) {
}

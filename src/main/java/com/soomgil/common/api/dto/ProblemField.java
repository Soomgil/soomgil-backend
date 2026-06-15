package com.soomgil.common.api.dto;

/**
 * validation 실패가 발생한 개별 입력 field 정보.
 *
 * <p>{@code name}은 가능한 한 frontend request field 이름과 맞추고,
 * {@code reason}은 사용자 또는 개발자가 실패 원인을 이해할 수 있는 짧은 설명을 담는다.
 */
public record ProblemField(
	String name,
	String reason
) {
}

package com.soomgil.common.cqrs;

/**
 * 응답 바디가 필요 없는 command handler의 명시적인 결과 타입.
 *
 * <p>handler 계약에서 {@code null}과 {@code Void} 사용을 피하기 위해 사용한다.
 */
public record NoResult() {

	/**
	 * 값이 없는 성공 결과를 표현하는 공유 instance.
	 */
	public static final NoResult INSTANCE = new NoResult();
}

package com.soomgil.common.time;

import java.time.Instant;

/**
 * 현재 시각을 제공하는 application 공통 계약.
 *
 * <p>도메인 로직에서 {@link Instant#now()}를 직접 호출하지 않고 이 계약을 사용하면
 * 테스트에서 고정된 시각을 주입할 수 있다. 저장 시각은 UTC {@link Instant}를 기본으로 한다.
 */
@FunctionalInterface
public interface TimeProvider {

	/**
	 * 현재 시각을 UTC 기준 {@link Instant}로 반환한다.
	 *
	 * @return 현재 시각
	 */
	Instant now();
}

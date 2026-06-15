package com.soomgil.common.time;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import org.springframework.stereotype.Component;

/**
 * 시스템 UTC clock을 사용하는 {@link TimeProvider} 구현체.
 *
 * <p>운영 코드에서는 Spring bean으로 주입하고, 테스트에서는 package-private 생성자에 고정 clock을 넣어 사용한다.
 */
@Component
public class SystemTimeProvider implements TimeProvider {

	private final Clock clock;

	public SystemTimeProvider() {
		this(Clock.systemUTC());
	}

	SystemTimeProvider(Clock clock) {
		this.clock = Objects.requireNonNull(clock, "clock must not be null");
	}

	@Override
	public Instant now() {
		return Instant.now(clock);
	}

	/**
	 * 이 provider가 사용하는 clock을 반환한다.
	 *
	 * @return 현재 clock
	 */
	Clock clock() {
		return clock;
	}
}

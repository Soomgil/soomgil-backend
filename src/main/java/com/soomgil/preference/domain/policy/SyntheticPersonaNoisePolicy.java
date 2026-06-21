package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * soft 페르소나 성향에 최대 5%의 결정적 잡음을 적용한다.
 *
 * <p>hard 성향이 일치한 장소는 절대 변경하지 않는다. 같은 페르소나, 장소, seed는
 * SHA-256 기반으로 항상 같은 잡음 적용 여부를 반환한다.
 */
public class SyntheticPersonaNoisePolicy {

	private final BigDecimal maximumNoiseRate;

	public SyntheticPersonaNoisePolicy() {
		this(new BigDecimal("0.05"));
	}

	public SyntheticPersonaNoisePolicy(BigDecimal maximumNoiseRate) {
		if (maximumNoiseRate == null
			|| maximumNoiseRate.compareTo(BigDecimal.ZERO) < 0
			|| maximumNoiseRate.compareTo(BigDecimal.ONE) > 0) {
			throw new IllegalArgumentException("maximum noise rate must be between 0 and 1");
		}
		this.maximumNoiseRate = maximumNoiseRate;
	}

	public BigDecimal apply(SyntheticPersonaNoiseInput input) {
		validate(input);
		if (input.hardPreferenceMatched() || input.noiseRate().compareTo(BigDecimal.ZERO) == 0) {
			return input.score();
		}
		return draw(input) < input.noiseRate().doubleValue()
			? input.score().negate()
			: input.score();
	}

	private double draw(SyntheticPersonaNoiseInput input) {
		String source = String.join(
			"\u001F",
			input.personaKey(),
			input.provider(),
			input.externalPlaceId(),
			Long.toString(input.seed()),
			"noise"
		);
		byte[] digest = sha256(source);
		long positive = ByteBuffer.wrap(digest).getLong() & Long.MAX_VALUE;
		return positive / (double) Long.MAX_VALUE;
	}

	private byte[] sha256(String source) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 must be available", exception);
		}
	}

	private void validate(SyntheticPersonaNoiseInput input) {
		Objects.requireNonNull(input, "input must not be null");
		Objects.requireNonNull(input.score(), "score must not be null");
		if (input.noiseRate() == null
			|| input.noiseRate().compareTo(BigDecimal.ZERO) < 0
			|| input.noiseRate().compareTo(maximumNoiseRate) > 0) {
			throw new IllegalArgumentException("noise rate must be between 0 and " + maximumNoiseRate);
		}
	}
}

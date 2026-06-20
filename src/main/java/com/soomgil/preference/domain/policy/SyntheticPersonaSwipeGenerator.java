package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * 페르소나 장소 점수와 hard 성향을 합성 스와이프 반응으로 변환한다.
 *
 * <p>중립 구간은 {@code personaKey + provider + externalPlaceId + seed}의 SHA-256 결과로 결정한다.
 * 따라서 같은 generator 입력은 실행 시점이나 서버와 관계없이 같은 반응을 반환한다.
 */
public class SyntheticPersonaSwipeGenerator {

	private final BigDecimal superLikeThreshold;
	private final BigDecimal likeThreshold;
	private final BigDecimal nopeThreshold;

	public SyntheticPersonaSwipeGenerator(
		BigDecimal superLikeThreshold,
		BigDecimal likeThreshold,
		BigDecimal nopeThreshold
	) {
		this.superLikeThreshold = Objects.requireNonNull(
			superLikeThreshold,
			"superLikeThreshold must not be null"
		);
		this.likeThreshold = Objects.requireNonNull(likeThreshold, "likeThreshold must not be null");
		this.nopeThreshold = Objects.requireNonNull(nopeThreshold, "nopeThreshold must not be null");
		if (superLikeThreshold.compareTo(likeThreshold) <= 0
			|| likeThreshold.compareTo(nopeThreshold) <= 0) {
			throw new IllegalArgumentException("thresholds must satisfy super like > like > nope");
		}
	}

	/**
	 * 합성 반응을 결정한다.
	 *
	 * <p>hard dislike는 항상 {@link SyntheticSwipeReaction#NOPE}다. hard like는 NOPE가 될 수 없지만,
	 * SUPER_LIKE는 장소 점수가 threshold 이상일 때만 반환한다.
	 *
	 * @param input 페르소나와 장소의 결정 입력
	 * @return 결정된 합성 반응
	 */
	public SyntheticSwipeReaction generate(SyntheticPersonaSwipeInput input) {
		validate(input);
		if (input.hardDislikeMatched()) {
			return SyntheticSwipeReaction.NOPE;
		}
		if (input.personaPlaceScore().compareTo(superLikeThreshold) >= 0) {
			return SyntheticSwipeReaction.SUPER_LIKE;
		}
		if (input.hardLikeMatched()) {
			return SyntheticSwipeReaction.LIKE;
		}
		if (input.personaPlaceScore().compareTo(likeThreshold) >= 0) {
			return SyntheticSwipeReaction.LIKE;
		}
		if (input.personaPlaceScore().compareTo(nopeThreshold) <= 0) {
			return SyntheticSwipeReaction.NOPE;
		}
		return deterministicNeutralReaction(input);
	}

	private SyntheticSwipeReaction deterministicNeutralReaction(SyntheticPersonaSwipeInput input) {
		String source = String.join(
			"\u001F",
			input.personaKey(),
			input.provider(),
			input.externalPlaceId(),
			Long.toString(input.seed())
		);
		byte[] digest = sha256(source);
		return (digest[0] & 1) == 0
			? SyntheticSwipeReaction.LIKE
			: SyntheticSwipeReaction.NOPE;
	}

	private byte[] sha256(String source) {
		try {
			return MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 must be available", exception);
		}
	}

	private void validate(SyntheticPersonaSwipeInput input) {
		Objects.requireNonNull(input, "input must not be null");
		if (input.personaKey() == null || input.personaKey().isBlank()) {
			throw new IllegalArgumentException("personaKey must not be blank");
		}
		if (input.provider() == null || input.provider().isBlank()) {
			throw new IllegalArgumentException("provider must not be blank");
		}
		if (input.externalPlaceId() == null || input.externalPlaceId().isBlank()) {
			throw new IllegalArgumentException("externalPlaceId must not be blank");
		}
		Objects.requireNonNull(input.personaPlaceScore(), "personaPlaceScore must not be null");
	}
}

package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 합성 스와이프 생성 전에 고정 페르소나 catalog의 불변 조건을 검사한다.
 *
 * <p>검증 실패 catalog는 합성 이벤트 생성이나 {@code SYNTHETIC_PERSONA} 통계 run에 사용할 수 없다.
 */
public class SyntheticPersonaCatalogValidator {

	private static final BigDecimal MAX_NOISE_RATE = new BigDecimal("0.05");

	private final int requiredPersonaCount;

	public SyntheticPersonaCatalogValidator(int requiredPersonaCount) {
		if (requiredPersonaCount < 1) {
			throw new IllegalArgumentException("required persona count must be positive");
		}
		this.requiredPersonaCount = requiredPersonaCount;
	}

	/**
	 * catalog의 인원수, key, hard 성향, 잡음 비율을 검증한다.
	 *
	 * @param personas 검증할 같은 generator version의 페르소나 목록
	 * @throws IllegalArgumentException catalog 불변 조건을 위반한 경우
	 */
	public void validate(List<SyntheticPersonaDefinition> personas) {
		if (personas == null || personas.size() != requiredPersonaCount) {
			throw new IllegalArgumentException(
				"synthetic persona catalog must contain exactly " + requiredPersonaCount + " personas"
			);
		}

		Set<String> personaKeys = new HashSet<>();
		for (SyntheticPersonaDefinition persona : personas) {
			if (persona == null || persona.personaKey().isBlank() || !personaKeys.add(persona.personaKey())) {
				throw new IllegalArgumentException("synthetic persona keys must be unique");
			}
			if (hasOverlap(persona.hardLikeTags(), persona.hardDislikeTags())) {
				throw new IllegalArgumentException("hard like and hard dislike tags must not overlap");
			}
			if (persona.noiseRate().compareTo(BigDecimal.ZERO) < 0
				|| persona.noiseRate().compareTo(MAX_NOISE_RATE) > 0) {
				throw new IllegalArgumentException("synthetic persona noise rate must be between 0 and 0.05");
			}
		}
	}

	private boolean hasOverlap(Set<String> first, Set<String> second) {
		return first.stream().anyMatch(second::contains);
	}
}

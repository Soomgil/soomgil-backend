package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Set;

/**
 * cold-start 합성 스와이프를 만드는 고정 페르소나 정의.
 *
 * <p>hard 성향은 반응 생성 중 절대 뒤집을 수 없고, soft 성향에만 {@code noiseRate} 범위의
 * 결정적 잡음을 적용할 수 있다. {@code seed}는 같은 입력에서 같은 반응을 재현하는 데 사용한다.
 *
 * @param personaKey generator version 안에서 유일한 페르소나 key
 * @param displayName 운영자가 구분할 수 있는 표시 이름
 * @param description 페르소나 성향 설명
 * @param hardLikeTags 반드시 긍정 반응해야 하는 태그
 * @param hardDislikeTags 반드시 부정 반응해야 하는 태그
 * @param softLikeTags 대체로 긍정 반응하는 태그
 * @param softDislikeTags 대체로 부정 반응하는 태그
 * @param neutralTags 중립 태그
 * @param noiseRate soft 성향에 허용할 잡음 비율
 * @param seed 결정적 반응 생성을 위한 seed
 */
public record SyntheticPersonaDefinition(
	String personaKey,
	String displayName,
	String description,
	Set<String> hardLikeTags,
	Set<String> hardDislikeTags,
	Set<String> softLikeTags,
	Set<String> softDislikeTags,
	Set<String> neutralTags,
	BigDecimal noiseRate,
	long seed
) {

	public SyntheticPersonaDefinition {
		Objects.requireNonNull(personaKey, "personaKey must not be null");
		Objects.requireNonNull(displayName, "displayName must not be null");
		Objects.requireNonNull(description, "description must not be null");
		hardLikeTags = Set.copyOf(Objects.requireNonNull(hardLikeTags, "hardLikeTags must not be null"));
		hardDislikeTags = Set.copyOf(Objects.requireNonNull(
			hardDislikeTags,
			"hardDislikeTags must not be null"
		));
		softLikeTags = Set.copyOf(Objects.requireNonNull(softLikeTags, "softLikeTags must not be null"));
		softDislikeTags = Set.copyOf(Objects.requireNonNull(
			softDislikeTags,
			"softDislikeTags must not be null"
		));
		neutralTags = Set.copyOf(Objects.requireNonNull(neutralTags, "neutralTags must not be null"));
		Objects.requireNonNull(noiseRate, "noiseRate must not be null");
	}
}

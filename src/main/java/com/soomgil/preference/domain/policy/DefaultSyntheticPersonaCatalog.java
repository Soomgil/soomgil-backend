package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * cold-start generator version {@code synthetic-persona-v1}의 고정 50명 catalog.
 *
 * <p>10개 여행 성향을 5개 고정 variant로 구성한다. variant는 seed와 noise rate가 달라
 * 같은 성향 안에서도 중립 및 soft 반응 분포가 한쪽으로 고정되지 않게 한다.
 */
public class DefaultSyntheticPersonaCatalog {

	public static final String GENERATOR_VERSION = "synthetic-persona-v1";

	private static final List<PersonaTemplate> TEMPLATES = List.of(
		template("nature-healer", "자연 힐링형", Set.of("nature", "forest"), Set.of("urban", "lively"),
			Set.of("scenic_view", "healing"), Set.of("indoor"), Set.of("museum")),
		template("culture-explorer", "문화 탐방형", Set.of("museum", "heritage_site"),
			Set.of("rides", "water_activity"), Set.of("history", "traditional"), Set.of("active"),
			Set.of("park")),
		template("active-adventurer", "활동 모험형", Set.of("hiking", "active"), Set.of("quiet", "indoor"),
			Set.of("cycling", "leisure_activity"), Set.of("museum"), Set.of("urban")),
		template("photo-hunter", "사진 명소형", Set.of("photo_spot", "scenic_view"),
			Set.of("indoor", "science_education"), Set.of("sunset", "night_view"), Set.of("learning"),
			Set.of("park")),
		template("family-experience", "가족 체험형", Set.of("park", "hands_on_experience"),
			Set.of("hiking", "hot_spring"), Set.of("animal_viewing", "picnic"), Set.of("night_view"),
			Set.of("museum")),
		template("coast-lover", "바다 휴양형", Set.of("coast", "waterfront"), Set.of("indoor", "urban"),
			Set.of("island", "sunset"), Set.of("museum"), Set.of("festival")),
		template("romantic-night", "로맨틱 야경형", Set.of("romantic", "night_view"),
			Set.of("rides", "lively"), Set.of("sunset", "artistic"), Set.of("hiking"), Set.of("observatory")),
		template("indoor-learner", "실내 학습형", Set.of("indoor", "science_education"),
			Set.of("hiking", "water_activity"), Set.of("learning", "museum"), Set.of("camping"),
			Set.of("park")),
		template("urban-architecture", "도시 건축형", Set.of("urban", "architecture"),
			Set.of("rural_landscape", "forest"), Set.of("modern", "street_alley"), Set.of("camping"),
			Set.of("gallery_exhibition")),
		template("quiet-rest", "조용한 휴식형", Set.of("quiet", "healing"), Set.of("lively", "festival"),
			Set.of("garden", "bookshop"), Set.of("rides"), Set.of("museum"))
	);

	private final List<SyntheticPersonaDefinition> personas;

	public DefaultSyntheticPersonaCatalog() {
		List<SyntheticPersonaDefinition> definitions = new ArrayList<>(50);
		int sequence = 1;
		for (PersonaTemplate template : TEMPLATES) {
			for (int variant = 1; variant <= 5; variant++) {
				definitions.add(new SyntheticPersonaDefinition(
					"%s-%02d".formatted(template.key(), variant),
					"%s %d".formatted(template.displayName(), variant),
					template.displayName() + "의 고정 cold-start 성향",
					template.hardLikeTags(),
					template.hardDislikeTags(),
					template.softLikeTags(),
					template.softDislikeTags(),
					template.neutralTags(),
					new BigDecimal("0.0" + variant),
					10_000L + sequence
				));
				sequence++;
			}
		}
		personas = List.copyOf(definitions);
	}

	/**
	 * generator version에 고정된 50명 catalog를 반환한다.
	 *
	 * @return 수정할 수 없는 페르소나 목록
	 */
	public List<SyntheticPersonaDefinition> personas() {
		return personas;
	}

	private static PersonaTemplate template(
		String key,
		String displayName,
		Set<String> hardLikeTags,
		Set<String> hardDislikeTags,
		Set<String> softLikeTags,
		Set<String> softDislikeTags,
		Set<String> neutralTags
	) {
		return new PersonaTemplate(
			key,
			displayName,
			hardLikeTags,
			hardDislikeTags,
			softLikeTags,
			softDislikeTags,
			neutralTags
		);
	}

	private record PersonaTemplate(
		String key,
		String displayName,
		Set<String> hardLikeTags,
		Set<String> hardDislikeTags,
		Set<String> softLikeTags,
		Set<String> softDislikeTags,
		Set<String> neutralTags
	) {
	}
}

package com.soomgil.preference.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 투표 스티커 개수를 취향 근거 단위로 환산하는 정책 테스트.
 *
 * <p>기존 projection 계산 공식은 바꾸지 않고, 스티커 개수를 어떤 가중치로 환산할지만 이 정책이 정한다.
 */
class TripVoteEvidencePolicyTest {

	private final TripVoteEvidencePolicy policy = new TripVoteEvidencePolicy(
		new BigDecimal("1.0"), new BigDecimal("0.5"), new BigDecimal("2.0")
	);

	@Test
	@DisplayName("스티커 1개는 LIKE와 같은 근거 강도(1.0)를 가진다")
	void singleStickerMatchesLikeStrength() {
		assertThat(policy.evidenceUnits(1)).isEqualByComparingTo("1.0");
	}

	@Test
	@DisplayName("스티커를 더 붙일수록 근거가 step만큼 커진다")
	void moreStickersIncreaseEvidence() {
		assertThat(policy.evidenceUnits(2)).isEqualByComparingTo("1.5");
		assertThat(policy.evidenceUnits(3)).isEqualByComparingTo("2.0");
	}

	@Test
	@DisplayName("근거는 SUPER_LIKE 강도(2.0)를 넘지 않도록 상한을 적용한다")
	void evidenceIsCappedAtMaxWeight() {
		assertThat(policy.evidenceUnits(4)).isEqualByComparingTo("2.0");
		assertThat(policy.evidenceUnits(10)).isEqualByComparingTo("2.0");
		assertThat(policy.evidenceUnits(1000)).isEqualByComparingTo("2.0");
	}

	@Test
	@DisplayName("스티커를 붙이지 않은 장소는 취향 반영 대상이 아니다")
	void nonPositiveStickerCountIsRejected() {
		assertThatThrownBy(() -> policy.evidenceUnits(0))
			.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> policy.evidenceUnits(-1))
			.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	@DisplayName("설정값을 바꾸면 환산 결과도 따라 바뀐다")
	void honoursConfiguredWeights() {
		TripVoteEvidencePolicy custom = new TripVoteEvidencePolicy(
			new BigDecimal("0.5"), new BigDecimal("0.25"), new BigDecimal("1.0")
		);

		assertThat(custom.evidenceUnits(1)).isEqualByComparingTo("0.5");
		assertThat(custom.evidenceUnits(2)).isEqualByComparingTo("0.75");
		assertThat(custom.evidenceUnits(5)).isEqualByComparingTo("1.0");
	}

	@Test
	@DisplayName("태그 근거는 장소 내 태그 비율에 환산 단위를 곱한 값이다")
	void distributesEvidenceAcrossTagsByRatio() {
		BigDecimal tagRatio = new BigDecimal("0.25");

		assertThat(policy.tagEvidence(tagRatio, 2)).isEqualByComparingTo("0.375");
	}

	@Test
	@DisplayName("계산 정책 버전을 노출해 감사와 재처리가 가능하다")
	void exposesCalculationVersion() {
		assertThat(policy.calculationVersion()).isEqualTo("trip-vote-evidence-v1");
	}
}

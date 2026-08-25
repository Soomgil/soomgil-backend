package com.soomgil.preference.domain.policy;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Objects;

/**
 * 여행 방 투표 스티커 개수를 개인 취향 근거 단위로 환산하는 정책.
 *
 * <p>기존 스와이프 projection 계산 공식은 바꾸지 않는다. 이 정책은 "스티커 몇 개가 어느 정도의 근거인가"만
 * 결정하고, 태그별 분배와 점수 재계산은 기존 {@code PlaceTagEvidenceCalculator}와
 * {@code UserPreferenceWeightCalculator}를 그대로 사용한다.
 *
 * <p>단위는 스와이프 근거와 같은 척도다. 기존 SQL이 LIKE에 evidence×1.0, SUPER_LIKE에 evidence×2.0을
 * 적용하므로 기본값도 스티커 1개를 LIKE 강도로, 상한을 SUPER_LIKE 강도로 맞춘다.
 *
 * <p>환산식: {@code units(n) = min(baseWeight + stickerStep × (n - 1), maxWeight)}
 *
 * <p>이 정책을 바꾸면 이미 반영된 근거는 자동으로 재계산되지 않으므로,
 * {@link #calculationVersion()}을 함께 올리고 재처리 대상을 식별할 수 있게 해야 한다.
 */
public class TripVoteEvidencePolicy {

	/** 이 환산식의 버전. 저장된 근거 row에 함께 기록해 재처리 대상을 식별한다. */
	public static final String CALCULATION_VERSION = "trip-vote-evidence-v1";

	private static final MathContext MATH_CONTEXT = new MathContext(16);

	private final BigDecimal baseWeight;
	private final BigDecimal stickerStep;
	private final BigDecimal maxWeight;

	/**
	 * 환산 가중치를 지정해 정책을 만든다.
	 *
	 * @param baseWeight 스티커 1개일 때의 근거 단위
	 * @param stickerStep 스티커가 하나 늘 때마다 더해지는 근거 단위
	 * @param maxWeight 한 장소가 가질 수 있는 근거 단위 상한
	 */
	public TripVoteEvidencePolicy(BigDecimal baseWeight, BigDecimal stickerStep, BigDecimal maxWeight) {
		this.baseWeight = Objects.requireNonNull(baseWeight, "baseWeight must not be null");
		this.stickerStep = Objects.requireNonNull(stickerStep, "stickerStep must not be null");
		this.maxWeight = Objects.requireNonNull(maxWeight, "maxWeight must not be null");
	}

	/**
	 * 스티커 개수를 근거 단위로 환산한다.
	 *
	 * @param stickerCount 한 장소에 붙인 스티커 개수. 1 이상이어야 한다
	 * @return 상한이 적용된 근거 단위
	 * @throws IllegalArgumentException 스티커 개수가 1보다 작을 때
	 */
	public BigDecimal evidenceUnits(int stickerCount) {
		if (stickerCount < 1) {
			throw new IllegalArgumentException("stickerCount must be greater than or equal to 1");
		}
		BigDecimal raw = baseWeight.add(stickerStep.multiply(BigDecimal.valueOf(stickerCount - 1L)));
		return raw.min(maxWeight);
	}

	/**
	 * 장소 안에서 한 태그가 차지하는 비율에 근거 단위를 곱해 태그별 근거를 계산한다.
	 *
	 * <p>{@code tagRatio}는 기존 {@code PlaceTagEvidenceCalculator}가 계산한, 장소 안에서 합이 1이 되는
	 * 태그 비율이다. 따라서 한 장소가 만들어내는 태그 근거의 합은 {@link #evidenceUnits(int)}와 같다.
	 *
	 * @param tagRatio 장소 내 태그 비율
	 * @param stickerCount 해당 장소에 붙인 스티커 개수
	 * @return 태그에 가산할 근거
	 */
	public BigDecimal tagEvidence(BigDecimal tagRatio, int stickerCount) {
		Objects.requireNonNull(tagRatio, "tagRatio must not be null");
		return tagRatio.multiply(evidenceUnits(stickerCount), MATH_CONTEXT);
	}

	/**
	 * 이 환산식의 버전을 반환한다.
	 *
	 * @return 저장된 근거 row에 기록할 계산 버전
	 */
	public String calculationVersion() {
		return CALCULATION_VERSION;
	}
}

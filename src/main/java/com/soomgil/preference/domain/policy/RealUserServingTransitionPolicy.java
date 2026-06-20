package com.soomgil.preference.domain.policy;

import java.util.List;

/**
 * 실제 사용자 통계를 serving으로 전환할 수 있는지 판단한다.
 *
 * <p>자동 전환은 전체 최종 반응 수와 모든 핵심 태그의 반응 수를 함께 충족해야 한다.
 * 검증된 offline evaluation을 운영자가 명시적으로 승인한 경우에만 표본 기준을 우회할 수 있다.
 */
public class RealUserServingTransitionPolicy {

	private final long minimumTotalReactionCount;
	private final long minimumCoreTagReactionCount;

	public RealUserServingTransitionPolicy(
		long minimumTotalReactionCount,
		long minimumCoreTagReactionCount
	) {
		if (minimumTotalReactionCount < 1 || minimumCoreTagReactionCount < 1) {
			throw new IllegalArgumentException("serving transition minimums must be positive");
		}
		this.minimumTotalReactionCount = minimumTotalReactionCount;
		this.minimumCoreTagReactionCount = minimumCoreTagReactionCount;
	}

	/**
	 * 실제 사용자 통계 serving 승격 가능 여부를 반환한다.
	 *
	 * @param totalReactionCount 실제 사용자 최종 반응 총수
	 * @param coreTagReactionCounts 활성·선택 가능 태그별 최종 반응 수
	 * @param offlineEvaluationApproved 운영자의 검증된 offline 평가 승인 여부
	 * @return serving 승격 가능하면 {@code true}
	 */
	public boolean canPromote(
		long totalReactionCount,
		List<Long> coreTagReactionCounts,
		boolean offlineEvaluationApproved
	) {
		if (offlineEvaluationApproved) {
			return true;
		}
		if (totalReactionCount < minimumTotalReactionCount
			|| coreTagReactionCounts == null
			|| coreTagReactionCounts.isEmpty()) {
			return false;
		}
		return coreTagReactionCounts.stream()
			.allMatch(count -> count != null && count >= minimumCoreTagReactionCount);
	}
}

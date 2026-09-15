package com.soomgil.preference.config;

import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 추천, 태그 선택, cold-start 전환에 사용하는 versioned 운영 설정.
 */
@Component
@ConfigurationProperties(prefix = "soomgil.preference")
public class PreferencePolicyProperties {

	private final Statistics statistics = new Statistics();
	private final TagSelection tagSelection = new TagSelection();
	private final Recommendation recommendation = new Recommendation();
	private final SyntheticPersona syntheticPersona = new SyntheticPersona();
	private final RealUser realUser = new RealUser();
	private final TripVote tripVote = new TripVote();

	public Statistics getStatistics() {
		return statistics;
	}

	public TagSelection getTagSelection() {
		return tagSelection;
	}

	public Recommendation getRecommendation() {
		return recommendation;
	}

	public SyntheticPersona getSyntheticPersona() {
		return syntheticPersona;
	}

	public RealUser getRealUser() {
		return realUser;
	}

	/**
	 * 여행 방 투표 스티커를 취향 근거로 환산할 때 사용하는 설정.
	 *
	 * @return 투표 근거 환산 설정
	 */
	public TripVote getTripVote() {
		return tripVote;
	}

	public static class Statistics {

		private BigDecimal alpha = new BigDecimal("100");

		public BigDecimal getAlpha() {
			return alpha;
		}

		public void setAlpha(BigDecimal alpha) {
			this.alpha = alpha;
		}
	}

	public static class TagSelection {

		private BigDecimal minimumConfidence = new BigDecimal("0.55");
		private int maximumConfirmedTags = 10;

		public BigDecimal getMinimumConfidence() {
			return minimumConfidence;
		}

		public void setMinimumConfidence(BigDecimal minimumConfidence) {
			this.minimumConfidence = minimumConfidence;
		}

		public int getMaximumConfirmedTags() {
			return maximumConfirmedTags;
		}

		public void setMaximumConfirmedTags(int maximumConfirmedTags) {
			this.maximumConfirmedTags = maximumConfirmedTags;
		}
	}

	public static class Recommendation {

		private BigDecimal matchedMemberThreshold = new BigDecimal("0.15");

		public BigDecimal getMatchedMemberThreshold() {
			return matchedMemberThreshold;
		}

		public void setMatchedMemberThreshold(BigDecimal matchedMemberThreshold) {
			this.matchedMemberThreshold = matchedMemberThreshold;
		}
	}

	public static class SyntheticPersona {

		private int requiredCount = 50;
		private BigDecimal maximumNoiseRate = new BigDecimal("0.05");
		private BigDecimal hardPreferenceStrength = new BigDecimal("1.50");
		private BigDecimal softPreferenceStrength = new BigDecimal("0.60");
		private BigDecimal superLikeThreshold = new BigDecimal("1.20");
		private BigDecimal likeThreshold = new BigDecimal("0.35");
		private BigDecimal nopeThreshold = new BigDecimal("-0.35");
		private long minimumCoreTagReactionCount = 50;

		public int getRequiredCount() {
			return requiredCount;
		}

		public void setRequiredCount(int requiredCount) {
			this.requiredCount = requiredCount;
		}

		public BigDecimal getMaximumNoiseRate() {
			return maximumNoiseRate;
		}

		public void setMaximumNoiseRate(BigDecimal maximumNoiseRate) {
			this.maximumNoiseRate = maximumNoiseRate;
		}

		public BigDecimal getHardPreferenceStrength() {
			return hardPreferenceStrength;
		}

		public void setHardPreferenceStrength(BigDecimal hardPreferenceStrength) {
			this.hardPreferenceStrength = hardPreferenceStrength;
		}

		public BigDecimal getSoftPreferenceStrength() {
			return softPreferenceStrength;
		}

		public void setSoftPreferenceStrength(BigDecimal softPreferenceStrength) {
			this.softPreferenceStrength = softPreferenceStrength;
		}

		public BigDecimal getSuperLikeThreshold() {
			return superLikeThreshold;
		}

		public void setSuperLikeThreshold(BigDecimal superLikeThreshold) {
			this.superLikeThreshold = superLikeThreshold;
		}

		public BigDecimal getLikeThreshold() {
			return likeThreshold;
		}

		public void setLikeThreshold(BigDecimal likeThreshold) {
			this.likeThreshold = likeThreshold;
		}

		public BigDecimal getNopeThreshold() {
			return nopeThreshold;
		}

		public void setNopeThreshold(BigDecimal nopeThreshold) {
			this.nopeThreshold = nopeThreshold;
		}

		public long getMinimumCoreTagReactionCount() {
			return minimumCoreTagReactionCount;
		}

		public void setMinimumCoreTagReactionCount(long minimumCoreTagReactionCount) {
			this.minimumCoreTagReactionCount = minimumCoreTagReactionCount;
		}
	}

	public static class RealUser {

		private long minimumTotalReactionCount = 10_000;
		private long minimumCoreTagReactionCount = 100;

		public long getMinimumTotalReactionCount() {
			return minimumTotalReactionCount;
		}

		public void setMinimumTotalReactionCount(long minimumTotalReactionCount) {
			this.minimumTotalReactionCount = minimumTotalReactionCount;
		}

		public long getMinimumCoreTagReactionCount() {
			return minimumCoreTagReactionCount;
		}

		public void setMinimumCoreTagReactionCount(long minimumCoreTagReactionCount) {
			this.minimumCoreTagReactionCount = minimumCoreTagReactionCount;
		}
	}

	/**
	 * 투표 스티커 개수를 취향 근거 단위로 환산하는 설정.
	 *
	 * <p>단위는 스와이프 근거와 같은 척도다. 기본값은 스티커 1개를 LIKE 강도(1.0)로,
	 * 상한을 SUPER_LIKE 강도(2.0)로 맞춘다. 값을 바꾸면 이미 반영된 근거는 재계산되지 않으므로
	 * {@code TripVoteEvidencePolicy}의 계산 버전과 함께 관리해야 한다.
	 */
	public static class TripVote {

		private BigDecimal baseWeight = new BigDecimal("1.0");
		private BigDecimal stickerStep = new BigDecimal("0.5");
		private BigDecimal maxWeight = new BigDecimal("2.0");

		public BigDecimal getBaseWeight() {
			return baseWeight;
		}

		public void setBaseWeight(BigDecimal baseWeight) {
			this.baseWeight = baseWeight;
		}

		public BigDecimal getStickerStep() {
			return stickerStep;
		}

		public void setStickerStep(BigDecimal stickerStep) {
			this.stickerStep = stickerStep;
		}

		public BigDecimal getMaxWeight() {
			return maxWeight;
		}

		public void setMaxWeight(BigDecimal maxWeight) {
			this.maxWeight = maxWeight;
		}
	}
}

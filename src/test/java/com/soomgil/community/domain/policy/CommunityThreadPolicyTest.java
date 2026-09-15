package com.soomgil.community.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommunityThreadPolicyTest {

	@Test
	@DisplayName("본문은 1자 이상 500자 이하만 허용한다")
	void acceptsContentWithinLengthRange() {
		assertThat(CommunityThreadPolicy.isValidContent("성심당 줄이 너무 길어요")).isTrue();
		assertThat(CommunityThreadPolicy.isValidContent("a")).isTrue();
		assertThat(CommunityThreadPolicy.isValidContent("가".repeat(500))).isTrue();
	}

	@Test
	@DisplayName("null, 공백만, 500자 초과 본문은 거부한다")
	void rejectsBlankOrTooLongContent() {
		assertThat(CommunityThreadPolicy.isValidContent(null)).isFalse();
		assertThat(CommunityThreadPolicy.isValidContent("")).isFalse();
		assertThat(CommunityThreadPolicy.isValidContent("   \n\t ")).isFalse();
		assertThat(CommunityThreadPolicy.isValidContent("가".repeat(501))).isFalse();
	}

	@Test
	@DisplayName("길이는 앞뒤 공백을 제거한 뒤 판단한다")
	void measuresLengthAfterStrip() {
		assertThat(CommunityThreadPolicy.isValidContent("  " + "가".repeat(500) + "  ")).isTrue();
	}

	@Test
	@DisplayName("이미지는 선택 항목이며 최대 4장까지 허용한다")
	void acceptsUpToFourImages() {
		assertThat(CommunityThreadPolicy.isValidMediaCount(0)).isTrue();
		assertThat(CommunityThreadPolicy.isValidMediaCount(4)).isTrue();
		assertThat(CommunityThreadPolicy.isValidMediaCount(5)).isFalse();
		assertThat(CommunityThreadPolicy.isValidMediaCount(-1)).isFalse();
	}

	@Test
	@DisplayName("답글 중첩은 1단계까지만 허용한다")
	void limitsReplyNestingToOneLevel() {
		assertThat(CommunityThreadPolicy.canReplyTo(0)).isTrue();
		assertThat(CommunityThreadPolicy.canReplyTo(1)).isFalse();
	}
}

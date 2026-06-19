package com.soomgil.community.domain.policy;

/**
 * 커뮤니티 게시글 입력값 검증 규칙.
 *
 * <p>DB 제약과 별개로 비즈니스 의미 단위 검증을 모아둔다.
 * Controller {@code @Valid} 어노테이션과互补적으로 handler/service에서 재검증한다.
 */
public final class CommunityPostPolicy {

	/** 제목 최소 길이. */
	public static final int TITLE_MIN = 1;
	/** 제목 최대 길이. */
	public static final int TITLE_MAX = 180;
	/** 요약 최대 길이. */
	public static final int SUMMARY_MAX = 1000;
	/** 해시태그 개별 이름 최대 길이. */
	public static final int HASHTAG_NAME_MAX = 60;
	/** 게시글당 해시태그 최대 개수. */
	public static final int HASHTAG_MAX_COUNT = 20;
	/** 게시글당 미디어 최대 개수. */
	public static final int MEDIA_MAX_COUNT = 30;
	/** 댓글 본문 최대 길이. */
	public static final int COMMENT_CONTENT_MAX = 2000;
	/** 댓글 중첩 최대 깊이 (0 = 최상위, 2 = 대대댓글까지 허용). */
	public static final int COMMENT_MAX_DEPTH = 2;

	private CommunityPostPolicy() {
	}

	/**
	 * 제목이 유효한지 검사.
	 *
	 * @param title 제목
	 * @return {@code null}이거나 길이가 {@value TITLE_MIN}~{@value TITLE_MAX} 범위면 true
	 */
	public static boolean isValidTitle(String title) {
		return title != null
			&& title.length() >= TITLE_MIN
			&& title.length() <= TITLE_MAX;
	}

	/**
	 * 요약 길이가 유효한지 검사.
	 *
	 * @param summary 요약 (nullable)
	 * @return {@code null}이거나 길이 ≤ {@value SUMMARY_MAX}이면 true
	 */
	public static boolean isValidSummary(String summary) {
		return summary == null || summary.length() <= SUMMARY_MAX;
	}
}

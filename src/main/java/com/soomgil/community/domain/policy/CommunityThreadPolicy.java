package com.soomgil.community.domain.policy;

/**
 * 커뮤니티 쓰레드와 답글의 입력 검증 규칙.
 *
 * <p>Threads/X형 공개 피드는 긴 글이 아니라 짧은 텍스트를 전제로 하므로 본문 길이를 500자로 제한한다.
 * DB CHECK 제약과 별개로 handler에서 마지막으로 한 번 더 보장하며, 실패는 {@code VALIDATION_FAILED}로
 * 변환되어 ProblemDetails 400으로 응답된다.
 *
 * <p>답글 중첩은 기존 게시글 댓글 정책과 같은 방향으로 1단계까지만 허용한다.
 */
public final class CommunityThreadPolicy {

	/** 쓰레드/답글 본문 최소 길이. */
	public static final int CONTENT_MIN = 1;
	/** 쓰레드/답글 본문 최대 길이. */
	public static final int CONTENT_MAX = 500;
	/** 쓰레드 하나에 첨부할 수 있는 이미지 최대 개수. */
	public static final int MEDIA_MAX_COUNT = 4;
	/** 답글 중첩 최대 깊이. 0은 root 답글, 1은 답글의 답글이다. */
	public static final int REPLY_MAX_DEPTH = 1;

	private CommunityThreadPolicy() {
	}

	/**
	 * 본문이 공개 가능한 길이인지 검사한다.
	 *
	 * @param content 검사할 본문. null과 공백만 있는 값은 허용하지 않는다
	 * @return 앞뒤 공백을 제거한 길이가 {@value CONTENT_MIN}~{@value CONTENT_MAX}면 true
	 */
	public static boolean isValidContent(String content) {
		if (content == null || content.isBlank()) {
			return false;
		}
		int length = content.strip().length();
		return length >= CONTENT_MIN && length <= CONTENT_MAX;
	}

	/**
	 * 첨부 이미지 개수가 허용 범위인지 검사한다.
	 *
	 * @param mediaCount 첨부 이미지 개수. 이미지는 선택 항목이므로 0도 유효하다
	 * @return 0 이상 {@value MEDIA_MAX_COUNT} 이하면 true
	 */
	public static boolean isValidMediaCount(int mediaCount) {
		return mediaCount >= 0 && mediaCount <= MEDIA_MAX_COUNT;
	}

	/**
	 * 부모 답글의 깊이를 기준으로 새 답글의 깊이가 허용되는지 검사한다.
	 *
	 * @param parentDepth 부모 답글의 깊이. root 답글을 작성할 때는 이 검사를 호출하지 않는다
	 * @return 새 답글 깊이가 {@value REPLY_MAX_DEPTH} 이하면 true
	 */
	public static boolean canReplyTo(int parentDepth) {
		return parentDepth + 1 <= REPLY_MAX_DEPTH;
	}
}

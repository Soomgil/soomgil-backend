package com.soomgil.community.api.dto;

/**
 * 신고/모더레이션 대상 종류.
 *
 * <p>{@code POST}와 {@code POST_COMMENT}는 deprecated된 여행 스냅샷 게시글 계열이고,
 * {@code THREAD}와 {@code THREAD_REPLY}는 신규 공개 피드의 쓰레드와 답글이다.
 * 값은 {@code community.content_reports.target_type}에 그대로 저장되므로 변경하지 않는다.
 */
public enum ReportTargetType {
	POST,
	POST_COMMENT,
	THREAD,
	THREAD_REPLY
}

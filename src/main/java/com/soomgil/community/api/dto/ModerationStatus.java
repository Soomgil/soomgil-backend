package com.soomgil.community.api.dto;

/**
 * 게시글/댓글의 모더레이션 노출 상태.
 *
 * <p>VISIBLE이 정상 노출, HIDDEN이 모더레이터 숨김, DELETED가 soft delete를 나타낸다.
 * 공개 feed에서는 VISIBLE인 콘텐츠만 조회된다.
 */
public enum ModerationStatus {
	VISIBLE,
	HIDDEN,
	DELETED
}

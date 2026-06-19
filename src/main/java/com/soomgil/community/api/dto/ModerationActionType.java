package com.soomgil.community.api.dto;

/**
 * 모더레이터가 적용할 수 있는 조치 유형.
 *
 * <p>HIDE는 콘텐츠를 공개 목록에서 숨기고, RESTORE는 숨겨진 콘텐츠를 다시 노출하며,
 * DELETE는 soft delete로 영구 비노출 처리한다.
 */
public enum ModerationActionType {
	HIDE,
	RESTORE,
	DELETE
}

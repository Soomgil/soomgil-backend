package com.soomgil.community.domain.model;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

/**
 * 커뮤니티 도메인에서 발생하는 비즈니스 예외.
 *
 * <p>{@link ErrorCode}에 정의된 community 전용 코드를 그대로 전달한다.
 * {@code GlobalExceptionHandler}가 {@code ProblemDetails}로 변환한다.
 */
public class CommunityException extends BusinessException {

	public CommunityException(ErrorCode errorCode) {
		super(errorCode);
	}
}

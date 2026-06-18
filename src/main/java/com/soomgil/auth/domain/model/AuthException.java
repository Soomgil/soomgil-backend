package com.soomgil.auth.domain.model;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

/**
 * auth 도메인에서 발생하는 business exception.
 *
 * <p>{@link BusinessException}을 상속하여 {@code GlobalExceptionHandler}가
 * ProblemDetails로 자동 변환하도록 한다.
 */
public class AuthException extends BusinessException {

	public AuthException(ErrorCode errorCode) {
		super(errorCode);
	}

	public AuthException(ErrorCode errorCode, String detail) {
		super(errorCode, detail);
	}
}

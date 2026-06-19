package com.soomgil.user.domain.model;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

/**
 * user 도메인에서 발생하는 business exception.
 *
 * <p>{@link BusinessException}을 상속하여 {@code GlobalExceptionHandler}가
 * {@code ProblemDetails}로 자동 변환하도록 한다. profile/settings 검증 실패, 계정 상태 충돌,
 * profile 부재 등 user 도메인 고유의 실패를 표현할 때 사용한다.
 */
public class UserException extends BusinessException {

	/**
	 * error code만으로 예외를 생성한다.
	 *
	 * @param errorCode user 도메인 실패 코드
	 */
	public UserException(ErrorCode errorCode) {
		super(errorCode);
	}

	/**
	 * error code와 이번 요청에 맞는 상세 메시지로 예외를 생성한다.
	 *
	 * @param errorCode user 도메인 실패 코드
	 * @param detail ProblemDetails의 {@code detail}로 사용될 메시지
	 */
	public UserException(ErrorCode errorCode, String detail) {
		super(errorCode, detail);
	}
}

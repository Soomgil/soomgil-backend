package com.soomgil.planning.domain.model;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

/**
 * planning 도메인에서 발생하는 비즈니스 예외.
 *
 * <p>{@link ErrorCode}에 정의된 planning 전용 코드를 그대로 전달한다.
 * {@code GlobalExceptionHandler}가 {@code ProblemDetails}로 변환한다.
 */
public class PlanningException extends BusinessException {

	/**
	 * 에러 코드를 지정해 예외를 생성한다.
	 *
	 * @param errorCode planning 전용 에러 코드
	 */
	public PlanningException(ErrorCode errorCode) {
		super(errorCode);
	}
}

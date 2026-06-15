package com.soomgil.global.error;

/**
 * 도메인 또는 application 규칙 위반을 표현하는 공통 runtime 예외.
 *
 * <p>handler/controller는 이 예외를 직접 catch하지 않고, {@link GlobalExceptionHandler}가
 * {@link com.soomgil.common.api.dto.ProblemDetails} 응답으로 변환하게 둔다.
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		this(errorCode, errorCode.defaultMessage());
	}

	/**
	 * error code와 이번 요청에 맞는 상세 메시지로 예외를 생성한다.
	 *
	 * @param errorCode 안정적인 실패 코드와 HTTP status
	 * @param message ProblemDetails의 detail로 사용될 메시지
	 */
	public BusinessException(ErrorCode errorCode, String message) {
		super(message);
		this.errorCode = errorCode;
	}

	/**
	 * 실패 응답 변환에 사용할 error code를 반환한다.
	 *
	 * @return 실패 코드
	 */
	public ErrorCode errorCode() {
		return errorCode;
	}
}

package com.soomgil.common.api;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

/**
 * API scaffold controller가 공유하는 보조 기능.
 *
 * <p>아직 구현되지 않은 endpoint는 이 support를 통해 같은 {@code NOT_IMPLEMENTED}
 * ProblemDetails 응답으로 떨어지게 한다.
 */
public abstract class ApiControllerSupport {

	/**
	 * scaffold만 존재하는 endpoint임을 알리는 예외를 던진다.
	 *
	 * @param <T> controller method의 선언 반환 타입
	 * @return 반환되지 않음
	 */
	protected final <T> T notImplemented() {
		throw new BusinessException(ErrorCode.NOT_IMPLEMENTED, "Endpoint contract is scaffolded only.");
	}
}

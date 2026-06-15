package com.soomgil.global.security;

import java.util.UUID;

/**
 * 현재 요청의 인증 사용자를 application 계층에 제공하는 계약.
 *
 * <p>실제 인증 구현이 완성되기 전에도 테스트에서는 fake provider를 주입해 command/query handler를 검증할 수 있다.
 */
@FunctionalInterface
public interface CurrentUserProvider {

	/**
	 * 현재 요청의 인증 사용자 정보를 반환한다.
	 *
	 * @return 현재 인증 사용자
	 */
	CurrentUser currentUser();

	/**
	 * 현재 사용자의 ID만 필요할 때 사용하는 편의 method.
	 *
	 * @return 현재 사용자 ID
	 */
	default UUID currentUserId() {
		return currentUser().userId();
	}
}

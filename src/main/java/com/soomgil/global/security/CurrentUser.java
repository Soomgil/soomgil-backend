package com.soomgil.global.security;

import java.util.Objects;
import java.util.UUID;

/**
 * 인증된 현재 사용자를 application 계층에 전달하기 위한 최소 identity.
 *
 * <p>{@code userId}는 필수이며, {@code email}은 인증 provider 또는 가입 흐름에 따라 없을 수 있다.
 * 도메인 로직은 이 타입에서 필요한 최소 사용자 식별 정보만 읽어야 한다.
 */
public record CurrentUser(
	UUID userId,
	String email
) {

	public CurrentUser {
		Objects.requireNonNull(userId, "userId must not be null");
	}
}

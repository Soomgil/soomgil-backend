package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;

/**
 * 이메일/비밀번호 로그인 요청.
 *
 * @param email 사용자 이메일
 * @param password raw 비밀번호
 */
public record LoginCommand(
	String email,
	String password
) implements Command<AuthTokenResult> {
}

package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;

/**
 * 이메일/비밀번호 회원가입 요청.
 *
 * @param email 사용자 이메일
 * @param password raw 비밀번호
 * @param displayName 표시 이름
 */
public record RegisterCommand(
	String email,
	String password,
	String displayName
) implements Command<RegisterResult> {
}

package com.soomgil.auth.application.command;

import com.soomgil.common.cqrs.Command;

/**
 * refresh token으로 access token 재발급 요청.
 *
 * @param refreshToken raw refresh token
 */
public record RefreshCommand(
	String refreshToken
) implements Command<AuthTokenResult> {
}

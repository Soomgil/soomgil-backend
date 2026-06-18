package com.soomgil.auth.application.command;

import com.soomgil.auth.api.dto.OAuthProviderCode;
import com.soomgil.common.cqrs.Command;

/**
 * OAuth 로그인 요청.
 *
 * @param provider OAuth 제공자
 * @param code authorization code
 * @param redirectUri 리다이렉트 URI
 */
public record OAuthLoginCommand(OAuthProviderCode provider, String code, String redirectUri) implements Command<AuthTokenResult> {
}

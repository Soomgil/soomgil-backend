package com.soomgil.global.security;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 검증된 {@link Jwt}를 {@link CurrentUser} principal을 가진 {@link CurrentUserAuthenticationToken}으로 변환한다.
 *
 * <p>JWT {@code sub} claim을 {@code userId}로, {@code email} claim을 선택적으로 사용한다.
 * 변환 실패({@code sub} 누락, UUID 아님) 시 {@link BadCredentialsException}을 던져
 * {@code AuthenticationEntryPoint}가 401 ProblemDetails 응답을 내보내게 한다.
 *
 * <p>이 converter는 JWT 검증 자체를 수행하지 않는다. 서명/만료/issuer 검증은 {@code JwtDecoder}가 담당한다.
 */
@Component
public class JwtToCurrentUserAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

	@Override
	public AbstractAuthenticationToken convert(Jwt jwt) {
		UUID userId = parseUserId(jwt.getSubject());
		String email = jwt.getClaimAsString("email");

		CurrentUser currentUser = new CurrentUser(userId, email);
		return new CurrentUserAuthenticationToken(
			currentUser,
			jwt,
			AuthorityUtils.NO_AUTHORITIES
		);
	}

	private UUID parseUserId(String subject) {
		if (!StringUtils.hasText(subject)) {
			throw new BadCredentialsException("JWT subject is missing");
		}
		try {
			return UUID.fromString(subject);
		} catch (IllegalArgumentException exception) {
			throw new BadCredentialsException("JWT subject must be a valid UUID", exception);
		}
	}
}

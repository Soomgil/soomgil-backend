package com.soomgil.global.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * {@link CurrentUser}를 principal로 가지는 인증 토큰.
 *
 * <p>Spring Security {@code oauth2ResourceServer().jwt()} 흐름에서 JWT가 검증된 뒤,
 * {@link JwtToCurrentUserAuthenticationConverter}가 이 토큰을 만들어 {@code SecurityContext}에 채운다.
 * controller는 {@code @AuthenticationPrincipal CurrentUser currentUser}로 현재 사용자를 주입받는다.
 */
public final class CurrentUserAuthenticationToken extends AbstractAuthenticationToken {

	private final CurrentUser principal;
	private final Jwt jwt;

	/**
	 * 인증된 토큰을 생성한다.
	 *
	 * @param principal 현재 사용자
	 * @param jwt 검증된 JWT 원본
	 * @param authorities 권한 목록 (없으면 빈 컬렉션)
	 */
	public CurrentUserAuthenticationToken(CurrentUser principal, Jwt jwt, Collection<? extends GrantedAuthority> authorities) {
		super(authorities);
		this.principal = principal;
		this.jwt = jwt;
		super.setAuthenticated(true);
	}

	@Override
	public Object getCredentials() {
		return jwt.getTokenValue();
	}

	@Override
	public Object getPrincipal() {
		return principal;
	}

	@Override
	public String getName() {
		return principal.userId().toString();
	}

	/**
	 * @return 검증된 JWT 원본
	 */
	public Jwt jwt() {
		return jwt;
	}
}

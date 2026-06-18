package com.soomgil.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 검증/발급에 사용하는 설정 값.
 *
 * @param secret HS256 서명에 사용할 base64 인코딩 32바이트 이상 시크릿
 * @param accessTokenTtlSeconds access token 수명 (초). 0 이하면 900초(15분) 기본값 적용
 * @param issuer JWT {@code iss} claim 값. 비어 있으면 {@code soomgil} 기본값 적용
 */
@ConfigurationProperties(prefix = "soomgil.security.jwt")
public record JwtProperties(
	String secret,
	long accessTokenTtlSeconds,
	String issuer
) {

	public JwtProperties {
		if (secret == null || secret.isBlank()) {
			throw new IllegalArgumentException("soomgil.security.jwt.secret must not be blank");
		}
		if (accessTokenTtlSeconds <= 0) {
			accessTokenTtlSeconds = 900;
		}
		if (issuer == null || issuer.isBlank()) {
			issuer = "soomgil";
		}
	}
}

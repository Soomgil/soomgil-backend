package com.soomgil.global.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.util.Assert;

/**
 * JWT 검증과 발급에 필요한 Spring bean을 구성한다.
 *
 * <p>HS256 대칭키를 기본으로 한다. 시크릿은 base64로 인코딩된 32바이트 이상 값을
 * {@code soomgil.security.jwt.secret}에서 받는다.
 * access token은 서명 검증과 함께 {@code iss} claim 일치까지 검증한다.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

	/**
	 * HS256 서명에 사용할 대칭 키.
	 *
	 * @param properties JWT 설정
	 * @return HmacSHA256 {@link SecretKey}
	 */
	@Bean
	public SecretKey jwtSecretKey(JwtProperties properties) {
		String secret = properties.secret();
		Assert.hasText(secret, "soomgil.security.jwt.secret must not be blank");

		byte[] bytes = java.util.Base64.getDecoder().decode(secret);
		Assert.isTrue(bytes.length >= 32, "soomgil.security.jwt.secret must be at least 32 bytes after base64 decode");

		return new SecretKeySpec(bytes, "HmacSHA256");
	}

	/**
	 * access token을 발급하는 encoder.
	 *
	 * @param jwtSecretKey HS256 대칭 키
	 * @return {@link NimbusJwtEncoder}
	 */
	@Bean
	public JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
		ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(jwtSecretKey);
		return new NimbusJwtEncoder(secret);
	}

	/**
	 * access token을 검증하는 decoder.
	 *
	 * <p>기본 검증(exp, iss)을 적용한다.
	 *
	 * @param jwtSecretKey HS256 대칭 키
	 * @param properties JWT 설정 (issuer 기준용)
	 * @return {@link NimbusJwtDecoder}
	 */
	@Bean
	public JwtDecoder jwtDecoder(SecretKey jwtSecretKey, JwtProperties properties) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(jwtSecretKey)
			.macAlgorithm(MacAlgorithm.HS256)
			.build();
		decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
		return decoder;
	}
}

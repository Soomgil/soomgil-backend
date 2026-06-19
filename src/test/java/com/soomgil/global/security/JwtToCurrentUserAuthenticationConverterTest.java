package com.soomgil.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * {@link JwtToCurrentUserAuthenticationConverter}의 JWT → {@link CurrentUser} 변환 계약을 검증한다.
 *
 * <p>converter는 JWT 검증 자체를 수행하지 않는다. 검증은 {@code JwtDecoder}가 담당하고,
 * converter는 이미 검증된 {@link Jwt}를 {@link CurrentUser}로 바꾸는 역할만 한다.
 */
class JwtToCurrentUserAuthenticationConverterTest {

	private final JwtToCurrentUserAuthenticationConverter converter = new JwtToCurrentUserAuthenticationConverter();

	@Test
	@DisplayName("JWT subject와 email claim을 CurrentUser로 변환한다")
	void convertsSubjectAndEmailToCurrentUser() {
		UUID userId = UUID.randomUUID();
		Jwt jwt = jwtBuilder()
			.subject(userId.toString())
			.claim("email", "user@example.com")
			.build();

		AbstractAuthenticationToken authentication = converter.convert(jwt);

		assertThat(authentication).isNotNull();
		assertThat(authentication.isAuthenticated()).isTrue();
		assertThat(authentication.getPrincipal()).isInstanceOf(CurrentUser.class);

		CurrentUser currentUser = (CurrentUser) authentication.getPrincipal();
		assertThat(currentUser.userId()).isEqualTo(userId);
		assertThat(currentUser.email()).isEqualTo("user@example.com");
	}

	@Test
	@DisplayName("email claim이 없으면 email이 null인 CurrentUser를 만든다")
	void missingEmailClaimResultsInNullEmail() {
		UUID userId = UUID.randomUUID();
		Jwt jwt = jwtBuilder()
			.subject(userId.toString())
			.build();

		CurrentUser currentUser = (CurrentUser) converter.convert(jwt).getPrincipal();

		assertThat(currentUser.userId()).isEqualTo(userId);
		assertThat(currentUser.email()).isNull();
	}

	@Test
	@DisplayName("subject가 없으면 예외를 던진다")
	void missingSubjectThrows() {
		Jwt jwt = jwtBuilder().build();

		assertThatThrownBy(() -> converter.convert(jwt))
			.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("subject가 UUID 형식이 아니면 예외를 던진다")
	void nonUuidSubjectThrows() {
		Jwt jwt = jwtBuilder()
			.subject("not-a-uuid")
			.build();

		assertThatThrownBy(() -> converter.convert(jwt))
			.isInstanceOf(Exception.class);
	}

	@Test
	@DisplayName("인증 토큰의 name은 userId 문자열이다")
	void tokenNameIsUserIdString() {
		UUID userId = UUID.randomUUID();
		Jwt jwt = jwtBuilder()
			.subject(userId.toString())
			.build();

		AbstractAuthenticationToken authentication = converter.convert(jwt);

		assertThat(authentication.getName()).isEqualTo(userId.toString());
	}

	private Jwt.Builder jwtBuilder() {
		Instant now = Instant.now();
		return Jwt.withTokenValue("dummy-token")
			.header("alg", "HS256")
			.issuedAt(now)
			.expiresAt(now.plusSeconds(900))
			.issuer("soomgil");
	}
}

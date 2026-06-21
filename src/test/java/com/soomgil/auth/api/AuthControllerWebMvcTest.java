package com.soomgil.auth.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.auth.api.dto.AuthTokenResponse;
import com.soomgil.auth.api.dto.EmailVerificationRequest;
import com.soomgil.auth.api.dto.LoginRequest;
import com.soomgil.auth.api.dto.LogoutRequest;
import com.soomgil.auth.api.dto.OAuthAuthorizationUrlResponse;
import com.soomgil.auth.api.dto.OAuthCallbackRequest;
import com.soomgil.auth.api.dto.OAuthProviderCode;
import com.soomgil.auth.api.dto.PasswordResetRequest;
import com.soomgil.auth.api.dto.PolicyDocument;
import com.soomgil.auth.api.dto.RefreshTokenRequest;
import com.soomgil.auth.api.dto.RegisterRequest;
import com.soomgil.auth.api.dto.ResetPasswordRequest;
import com.soomgil.auth.api.dto.VerifyEmailRequest;
import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.LoginCommand;
import com.soomgil.auth.application.command.LogoutCommand;
import com.soomgil.auth.application.command.OAuthLoginCommand;
import com.soomgil.auth.application.command.RefreshCommand;
import com.soomgil.auth.application.command.RegisterCommand;
import com.soomgil.auth.application.command.RegisterResult;
import com.soomgil.auth.application.command.RequestPasswordResetCommand;
import com.soomgil.auth.application.command.ResetPasswordCommand;
import com.soomgil.auth.application.command.SendEmailVerificationCommand;
import com.soomgil.auth.application.command.VerifyEmailCommand;
import com.soomgil.auth.application.handler.GetCurrentUserQueryHandler;
import com.soomgil.auth.application.handler.ListPoliciesQueryHandler;
import com.soomgil.auth.application.handler.LoginCommandHandler;
import com.soomgil.auth.application.handler.LogoutCommandHandler;
import com.soomgil.auth.application.handler.OAuthLoginCommandHandler;
import com.soomgil.auth.application.handler.RefreshCommandHandler;
import com.soomgil.auth.application.handler.RegisterCommandHandler;
import com.soomgil.auth.application.handler.RequestPasswordResetCommandHandler;
import com.soomgil.auth.application.handler.ResetPasswordCommandHandler;
import com.soomgil.auth.application.handler.SendEmailVerificationCommandHandler;
import com.soomgil.auth.application.handler.VerifyEmailCommandHandler;
import com.soomgil.auth.application.query.GetCurrentUserQuery;
import com.soomgil.auth.application.query.ListPoliciesQuery;
import com.soomgil.auth.application.service.OAuthClient;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.JwtProperties;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserProfile;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.api.dto.UserStatus;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * {@link AuthController} 웹 계층 슬라이스 테스트.
 *
 * <p>handler 빈을 Mockito {@link MockBean}으로 교체하여 HTTP 계약과 ProblemDetails 변환을 검증한다.
 * {@code api_spec.md}가 정의한 URL 경로가 정확히 노출되는지 확인한다.
 */
@WebMvcTest(controllers = AuthController.class)
@Import({
	AuthControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class AuthControllerWebMvcTest {

	private static final UUID AUTHENTICATED_USER_ID = UUID.randomUUID();
	private static final CurrentUser AUTHENTICATED_USER = new CurrentUser(AUTHENTICATED_USER_ID, "viewer@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private RegisterCommandHandler registerCommandHandler;
	@MockBean
	private LoginCommandHandler loginCommandHandler;
	@MockBean
	private RefreshCommandHandler refreshCommandHandler;
	@MockBean
	private LogoutCommandHandler logoutCommandHandler;
	@MockBean
	private SendEmailVerificationCommandHandler sendEmailVerificationCommandHandler;
	@MockBean
	private VerifyEmailCommandHandler verifyEmailCommandHandler;
	@MockBean
	private RequestPasswordResetCommandHandler requestPasswordResetCommandHandler;
	@MockBean
	private ResetPasswordCommandHandler resetPasswordCommandHandler;
	@MockBean
	private OAuthLoginCommandHandler oauthLoginCommandHandler;
	@MockBean
	private ListPoliciesQueryHandler listPoliciesQueryHandler;
	@MockBean
	private GetCurrentUserQueryHandler getCurrentUserQueryHandler;
	@MockBean
	private OAuthClient oauthClient;
	@MockBean
	private JwtProperties jwtProperties;
	@MockBean
	private com.soomgil.auth.application.handler.OnboardCommandHandler onboardCommandHandler;

	static RequestPostProcessor asCurrentUser() {
		org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
			new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				AUTHENTICATED_USER, null, java.util.List.of()
			);
		return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
			.authentication(authentication);
	}

	@Test
	@DisplayName("POST /register - 정상 요청은 201과 RegisterResponse(userId, email, message)를 반환한다")
	void registerReturns201() throws Exception {
		when(registerCommandHandler.handle(any(RegisterCommand.class)))
			.thenReturn(new RegisterResult(AUTHENTICATED_USER_ID, "minji@example.com"));

		RegisterRequest body = sampleRegisterRequest();

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.userId").exists())
			.andExpect(jsonPath("$.email").value("minji@example.com"))
			.andExpect(jsonPath("$.message").exists());
	}

	@Test
	@DisplayName("POST /register - 이미 가입된 이메일이면 409 EMAIL_ALREADY_USED를 반환한다")
	void registerRejectsDuplicateEmail() throws Exception {
		when(registerCommandHandler.handle(any(RegisterCommand.class)))
			.thenThrow(new BusinessException(ErrorCode.EMAIL_ALREADY_USED));

		RegisterRequest body = sampleRegisterRequest();

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.code").value("EMAIL_ALREADY_USED"));
	}

	@Test
	@DisplayName("POST /login - 정상 요청은 200을 반환한다")
	void loginReturns200() throws Exception {
		when(loginCommandHandler.handle(any(LoginCommand.class))).thenReturn(sampleTokenResult());
		when(jwtProperties.accessTokenTtlSeconds()).thenReturn(900L);

		LoginRequest body = new LoginRequest("minji@example.com", "password123");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	@DisplayName("POST /login - 인증 실패 시 401 INVALID_CREDENTIALS를 반환한다")
	void loginRejectsInvalidCredentials() throws Exception {
		when(loginCommandHandler.handle(any(LoginCommand.class)))
			.thenThrow(new BusinessException(ErrorCode.INVALID_CREDENTIALS));

		LoginRequest body = new LoginRequest("minji@example.com", "wrong-password");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
	}

	@Test
	@DisplayName("POST /login - PENDING 계정(미인증)은 403 EMAIL_NOT_VERIFIED를 반환한다")
	void loginRejectsPendingUser() throws Exception {
		when(loginCommandHandler.handle(any(LoginCommand.class)))
			.thenThrow(new BusinessException(ErrorCode.EMAIL_NOT_VERIFIED));

		LoginRequest body = new LoginRequest("minji@example.com", "password123");

		mockMvc.perform(post("/api/v1/auth/login")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("EMAIL_NOT_VERIFIED"));
	}

	@Test
	@DisplayName("POST /refresh - 정상 요청은 200을 반환한다 (api_spec URL)")
	void refreshUsesApiSpecUrl() throws Exception {
		when(refreshCommandHandler.handle(any(RefreshCommand.class))).thenReturn(sampleTokenResult());
		when(jwtProperties.accessTokenTtlSeconds()).thenReturn(900L);

		RefreshTokenRequest body = new RefreshTokenRequest("opaque-refresh-token");

		mockMvc.perform(post("/api/v1/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	@DisplayName("POST /logout - 정상 요청은 204를 반환한다")
	void logoutReturns204() throws Exception {
		LogoutRequest body = new LogoutRequest("opaque-refresh-token", false);

		mockMvc.perform(post("/api/v1/auth/logout")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("POST /logout - 인증 없이 호출하면 401을 반환한다")
	void logoutWithoutAuthReturns401() throws Exception {
		LogoutRequest body = new LogoutRequest("opaque-refresh-token", false);

		mockMvc.perform(post("/api/v1/auth/logout")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("POST /email-verification-requests - 정상 요청은 202를 반환한다 (api_spec URL)")
	void sendEmailVerificationUsesApiSpecUrl() throws Exception {
		EmailVerificationRequest body = new EmailVerificationRequest("minji@example.com");

		mockMvc.perform(post("/api/v1/auth/email-verification-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isAccepted());
	}

	@Test
	@DisplayName("POST /email-verifications - 정상 요청은 200과 User를 반환한다 (api_spec URL)")
	void verifyEmailUsesApiSpecUrl() throws Exception {
		when(verifyEmailCommandHandler.handle(any(VerifyEmailCommand.class)))
			.thenReturn(AUTHENTICATED_USER_ID);
		when(getCurrentUserQueryHandler.handle(any(GetCurrentUserQuery.class)))
			.thenReturn(sampleUser());

		VerifyEmailRequest body = new VerifyEmailRequest("verification-token");

		mockMvc.perform(post("/api/v1/auth/email-verifications")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").exists());
	}

	@Test
	@DisplayName("POST /password-reset-requests - 정상 요청은 202를 반환한다 (api_spec URL)")
	void requestPasswordResetUsesApiSpecUrl() throws Exception {
		PasswordResetRequest body = new PasswordResetRequest("minji@example.com");

		mockMvc.perform(post("/api/v1/auth/password-reset-requests")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isAccepted());
	}

	@Test
	@DisplayName("POST /password-resets - 정상 요청은 204를 반환한다 (api_spec URL)")
	void resetPasswordUsesApiSpecUrl() throws Exception {
		ResetPasswordRequest body = new ResetPasswordRequest("reset-token", "newPassword123");

		mockMvc.perform(post("/api/v1/auth/password-resets")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("GET /policy-documents - 정상 요청은 200과 PolicyDocument 목록을 반환한다 (api_spec URL)")
	void listPoliciesUsesApiSpecUrl() throws Exception {
		PolicyDocument doc = new PolicyDocument(
			UUID.randomUUID(), "TERMS_OF_SERVICE", "1.0", "ko", "이용약관",
			URI.create("https://cdn.example.com/tos-ko.md"), "hash", true,
			OffsetDateTime.now()
		);
		when(listPoliciesQueryHandler.handle(any(ListPoliciesQuery.class))).thenReturn(List.of(doc));

		mockMvc.perform(get("/api/v1/auth/policy-documents"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].policyCode").value("TERMS_OF_SERVICE"));
	}

	@Test
	@DisplayName("GET /oauth/{provider}/authorization-url - 정상 요청은 200과 인증 URL을 반환한다")
	void oauthAuthorizationUrlReturns200() throws Exception {
		when(oauthClient.getAuthorizationUrl(any(OAuthProviderCode.class), anyString(), nullable(String.class)))
			.thenReturn("https://kauth.kakao.com/oauth/authorize?client_id=x");

		mockMvc.perform(get("/api/v1/auth/oauth/KAKAO/authorization-url")
				.param("redirectUri", "https://app.example.com/callback"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.authorizationUrl").exists())
			.andExpect(jsonPath("$.state").exists());
	}

	@Test
	@DisplayName("POST /oauth/{provider}/callback - 정상 요청은 200과 토큰을 반환한다")
	void oauthCallbackReturns200() throws Exception {
		when(oauthLoginCommandHandler.handle(any(OAuthLoginCommand.class))).thenReturn(sampleTokenResult());
		when(jwtProperties.accessTokenTtlSeconds()).thenReturn(900L);

		OAuthCallbackRequest body = new OAuthCallbackRequest(
			"auth-code", URI.create("https://app.example.com/callback"), null, null
		);

		mockMvc.perform(post("/api/v1/auth/oauth/KAKAO/callback")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.accessToken").exists());
	}

	@Test
	@DisplayName("POST /register - 잘못된 이메일 형식이면 400 VALIDATION_FAILED를 반환한다")
	void registerRejectsInvalidEmail() throws Exception {
		RegisterRequest body = new RegisterRequest(
			"not-an-email", "password123", "민지", null, null, null, List.of(UUID.randomUUID())
		);

		mockMvc.perform(post("/api/v1/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	private RegisterRequest sampleRegisterRequest() {
		return new RegisterRequest(
			"minji@example.com", "password123", "민지",
			null, null, null, List.of(UUID.randomUUID())
		);
	}

	private AuthTokenResult sampleTokenResult() {
		return new AuthTokenResult(
			"access-token-value",
			"refresh-token-value",
			AUTHENTICATED_USER_ID,
			"minji@example.com",
			"민지",
			true
		);
	}

	private User sampleUser() {
		UserProfile profile = new UserProfile("민지", null, null, null, UserProfileVisibility.PUBLIC);
		UserSettings settings = new UserSettings("ko", "Asia/Seoul", false, null, null, true);
		return new User(
			AUTHENTICATED_USER_ID,
			"minji@example.com",
			null,
			UserStatus.ACTIVE,
			null,
			null,
			null,
			profile,
			settings,
			OffsetDateTime.now()
		);
	}

	@TestConfiguration
	static class TestSecurityConfig {

		@Bean
		SecurityFilterChain testSecurityFilterChain(
			HttpSecurity http,
			ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
			ProblemDetailsAccessDeniedHandler accessDeniedHandler
		) throws Exception {
			return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(exception -> exception
					.authenticationEntryPoint(authenticationEntryPoint)
					.accessDeniedHandler(accessDeniedHandler)
				)
				.authorizeHttpRequests(auth -> auth
					.requestMatchers("/actuator/health/**").permitAll()
					// api_spec 기준 공개 엔드포인트. 인증이 필요한 엔드포인트(/logout)는
					// 개별 테스트에서 asCurrentUser()를 명시적으로 사용한다.
					.requestMatchers("/api/v1/auth/register").permitAll()
					.requestMatchers("/api/v1/auth/login").permitAll()
					.requestMatchers("/api/v1/auth/refresh").permitAll()
					.requestMatchers("/api/v1/auth/email-verification-requests").permitAll()
					.requestMatchers("/api/v1/auth/email-verifications").permitAll()
					.requestMatchers("/api/v1/auth/password-reset-requests").permitAll()
					.requestMatchers("/api/v1/auth/password-resets").permitAll()
					.requestMatchers("/api/v1/auth/policy-documents").permitAll()
					.requestMatchers("/api/v1/auth/oauth/**").permitAll()
					.anyRequest().authenticated()
				)
				.build();
		}
	}
}

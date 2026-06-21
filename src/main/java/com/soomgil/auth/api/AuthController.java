package com.soomgil.auth.api;

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
import com.soomgil.auth.api.dto.RegisterResponse;
import com.soomgil.auth.api.dto.ResetPasswordRequest;
import com.soomgil.auth.api.dto.VerifyEmailRequest;
import com.soomgil.auth.api.dto.OnboardRequest;
import com.soomgil.auth.application.command.AuthTokenResult;
import com.soomgil.auth.application.command.LoginCommand;
import com.soomgil.auth.application.command.RegisterResult;
import com.soomgil.auth.application.command.LogoutCommand;
import com.soomgil.auth.application.command.OAuthLoginCommand;
import com.soomgil.auth.application.command.RefreshCommand;
import com.soomgil.auth.application.command.RegisterCommand;
import com.soomgil.auth.application.command.OnboardCommand;
import com.soomgil.auth.application.command.RequestPasswordResetCommand;
import com.soomgil.auth.application.command.ResetPasswordCommand;
import com.soomgil.auth.application.command.SendEmailVerificationCommand;
import com.soomgil.auth.application.command.VerifyEmailCommand;
import com.soomgil.auth.application.handler.GetCurrentUserQueryHandler;
import com.soomgil.auth.application.handler.OnboardCommandHandler;
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
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.JwtProperties;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserProfile;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.api.dto.UserStatus;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 도메인의 REST 엔드포인트.
 *
 * <p>{@code /api/v1/auth/*} 경로 아래 회원가입, 로그인, 토큰 갱신, 로그아웃, 이메일 인증,
 * 비밀번호 재설정, OAuth 로그인, 약관 조회를 담당한다. 세션/보안 이벤트 조회는
 * {@code /api/v1/me/*} 네임스페이스로 {@link com.soomgil.user.api.UserController}에 있다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends ApiControllerSupport {

	private final RegisterCommandHandler registerCommandHandler;
	private final LoginCommandHandler loginCommandHandler;
	private final RefreshCommandHandler refreshCommandHandler;
	private final LogoutCommandHandler logoutCommandHandler;
	private final SendEmailVerificationCommandHandler sendEmailVerificationCommandHandler;
	private final VerifyEmailCommandHandler verifyEmailCommandHandler;
	private final RequestPasswordResetCommandHandler requestPasswordResetCommandHandler;
	private final ResetPasswordCommandHandler resetPasswordCommandHandler;
	private final OAuthLoginCommandHandler oauthLoginCommandHandler;
	private final OnboardCommandHandler onboardCommandHandler;
	private final ListPoliciesQueryHandler listPoliciesQueryHandler;
	private final GetCurrentUserQueryHandler getCurrentUserQueryHandler;
	private final OAuthClient oauthClient;
	private final JwtProperties jwtProperties;

	public AuthController(
		RegisterCommandHandler registerCommandHandler,
		LoginCommandHandler loginCommandHandler,
		RefreshCommandHandler refreshCommandHandler,
		LogoutCommandHandler logoutCommandHandler,
		SendEmailVerificationCommandHandler sendEmailVerificationCommandHandler,
		VerifyEmailCommandHandler verifyEmailCommandHandler,
		RequestPasswordResetCommandHandler requestPasswordResetCommandHandler,
		ResetPasswordCommandHandler resetPasswordCommandHandler,
		OAuthLoginCommandHandler oauthLoginCommandHandler,
		OnboardCommandHandler onboardCommandHandler,
		ListPoliciesQueryHandler listPoliciesQueryHandler,
		GetCurrentUserQueryHandler getCurrentUserQueryHandler,
		OAuthClient oauthClient,
		JwtProperties jwtProperties
	) {
		this.registerCommandHandler = registerCommandHandler;
		this.loginCommandHandler = loginCommandHandler;
		this.refreshCommandHandler = refreshCommandHandler;
		this.logoutCommandHandler = logoutCommandHandler;
		this.sendEmailVerificationCommandHandler = sendEmailVerificationCommandHandler;
		this.verifyEmailCommandHandler = verifyEmailCommandHandler;
		this.requestPasswordResetCommandHandler = requestPasswordResetCommandHandler;
		this.resetPasswordCommandHandler = resetPasswordCommandHandler;
		this.oauthLoginCommandHandler = oauthLoginCommandHandler;
		this.onboardCommandHandler = onboardCommandHandler;
		this.listPoliciesQueryHandler = listPoliciesQueryHandler;
		this.getCurrentUserQueryHandler = getCurrentUserQueryHandler;
		this.oauthClient = oauthClient;
		this.jwtProperties = jwtProperties;
	}

	@PostMapping("/login")
	public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
		LoginCommand command = new LoginCommand(request.email(), request.password());
		return toAuthTokenResponse(loginCommandHandler.handle(command));
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
		RegisterCommand command = new RegisterCommand(
			request.email(), request.password(), request.displayName(), request.acceptedPolicyDocumentIds()
		);
		RegisterResult result = registerCommandHandler.handle(command);
		return new RegisterResponse(
			result.userId(),
			result.email(),
			"이메일 인증 메일이 발송되었습니다. 메일함을 확인해주세요."
		);
	}

	@PostMapping("/refresh")
	public AuthTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		RefreshCommand command = new RefreshCommand(request.refreshToken());
		return toAuthTokenResponse(refreshCommandHandler.handle(command));
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(
		@Valid @RequestBody LogoutRequest request,
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser
	) {
		boolean allDevices = request.allDevices() != null && request.allDevices();
		LogoutCommand command = new LogoutCommand(currentUser.userId(), request.refreshToken(), allDevices);
		logoutCommandHandler.handle(command);
	}

	@PostMapping("/email-verification-requests")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
		SendEmailVerificationCommand command = new SendEmailVerificationCommand(request.email());
		sendEmailVerificationCommandHandler.handle(command);
	}

	@PostMapping("/email-verifications")
	public User verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		VerifyEmailCommand command = new VerifyEmailCommand(request.token());
		UUID userId = verifyEmailCommandHandler.handle(command);
		return getCurrentUserQueryHandler.handle(new GetCurrentUserQuery(userId));
	}

	@PostMapping("/password-reset-requests")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		RequestPasswordResetCommand command = new RequestPasswordResetCommand(request.email());
		requestPasswordResetCommandHandler.handle(command);
	}

	@PostMapping("/password-resets")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		ResetPasswordCommand command = new ResetPasswordCommand(request.token(), request.newPassword());
		resetPasswordCommandHandler.handle(command);
	}

	@GetMapping("/oauth/{provider}/authorization-url")
	public OAuthAuthorizationUrlResponse createOAuthAuthorizationUrl(
		@PathVariable OAuthProviderCode provider,
		@RequestParam URI redirectUri,
		@RequestParam(required = false) String state
	) {
		// state를 한 번만 생성해서 URL과 응답 body에 동일 값 사용.
		// 순서가 바뀌면 OAuthClient 내부에서 새 UUID를 만들어 URL에 넣고,
		// body엔 또 다른 UUID가 들어가 콜백 시 state 불일치로 CSRF 검증이 실패한다.
		String actualState = state != null ? state : UUID.randomUUID().toString();
		String url = oauthClient.getAuthorizationUrl(provider, redirectUri.toString(), actualState);
		return new OAuthAuthorizationUrlResponse(URI.create(url), actualState);
	}

	@PostMapping("/oauth/{provider}/callback")
	public AuthTokenResponse completeOAuthLogin(
		@PathVariable OAuthProviderCode provider,
		@Valid @RequestBody OAuthCallbackRequest request
	) {
		OAuthLoginCommand command = new OAuthLoginCommand(
			provider, request.code(), request.redirectUri().toString()
		);
		return toAuthTokenResponse(oauthLoginCommandHandler.handle(command));
	}

	@PostMapping("/onboard")
	public AuthTokenResponse onboard(
		@org.springframework.security.core.annotation.AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody OnboardRequest request
	) {
		OnboardCommand command = new OnboardCommand(
			currentUser.userId(),
			request.displayName(),
			request.acceptedPolicyDocumentIds()
		);
		return toAuthTokenResponse(onboardCommandHandler.handle(command));
	}

	@GetMapping("/policy-documents")
	public List<PolicyDocument> listPolicies(
		@RequestParam(required = false) String languageCode,
		@RequestParam(required = false) Boolean requiredOnly
	) {
		ListPoliciesQuery query = new ListPoliciesQuery(
			languageCode,
			requiredOnly != null && requiredOnly
		);
		return listPoliciesQueryHandler.handle(query);
	}

	private AuthTokenResponse toAuthTokenResponse(AuthTokenResult result) {
		UserProfile profile = new UserProfile(
			result.displayName(), null, null, null, UserProfileVisibility.PUBLIC
		);
		UserSettings settings = new UserSettings("ko", "Asia/Seoul", false, null, null, true);
		UserStatus status = result.onboarded() ? UserStatus.ACTIVE : UserStatus.PENDING_ONBOARDING;
		User user = new User(
			result.userId(),
			result.email(),
			null,
			status,
			null,
			null,
			null,
			profile,
			settings,
			OffsetDateTime.now()
		);
		return new AuthTokenResponse(
			result.accessToken(),
			result.refreshToken(),
			"Bearer",
			(int) jwtProperties.accessTokenTtlSeconds(),
			user,
			result.onboarded()
		);
	}
}

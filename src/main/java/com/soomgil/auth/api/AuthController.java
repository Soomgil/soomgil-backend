package com.soomgil.auth.api;

import com.soomgil.auth.api.dto.AuthTokenResponse;
import com.soomgil.auth.api.dto.EmailVerificationRequest;
import com.soomgil.auth.api.dto.LoginRequest;
import com.soomgil.auth.api.dto.LogoutRequest;
import com.soomgil.auth.api.dto.OAuthAuthorizationUrlResponse;
import com.soomgil.auth.api.dto.OAuthCallbackRequest;
import com.soomgil.auth.api.dto.OAuthProviderCode;
import com.soomgil.auth.api.dto.PagedSecurityEvent;
import com.soomgil.auth.api.dto.PagedUserSession;
import com.soomgil.auth.api.dto.PasswordResetRequest;
import com.soomgil.auth.api.dto.PolicyDocument;
import com.soomgil.auth.api.dto.RefreshTokenRequest;
import com.soomgil.auth.api.dto.RegisterRequest;
import com.soomgil.auth.api.dto.ResetPasswordRequest;
import com.soomgil.auth.api.dto.VerifyEmailRequest;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.user.api.dto.User;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController extends ApiControllerSupport {

	@PostMapping("/login")
	public AuthTokenResponse login(@Valid @RequestBody LoginRequest request) {
		return notImplemented();
	}

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public AuthTokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return notImplemented();
	}

	@PostMapping("/token/refresh")
	public AuthTokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
		return notImplemented();
	}

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(@Valid @RequestBody LogoutRequest request) {
		notImplemented();
	}

	@PostMapping("/email-verifications")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void sendEmailVerification(@Valid @RequestBody EmailVerificationRequest request) {
		notImplemented();
	}

	@PostMapping("/email-verifications/verify")
	public User verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
		return notImplemented();
	}

	@PostMapping("/password-resets")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public void requestPasswordReset(@Valid @RequestBody PasswordResetRequest request) {
		notImplemented();
	}

	@PostMapping("/password-resets/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
		notImplemented();
	}

	@GetMapping("/oauth/{provider}/authorization-url")
	public OAuthAuthorizationUrlResponse createOAuthAuthorizationUrl(
		@PathVariable OAuthProviderCode provider,
		@RequestParam URI redirectUri,
		@RequestParam(required = false) String state
	) {
		return notImplemented();
	}

	@PostMapping("/oauth/{provider}/callback")
	public AuthTokenResponse completeOAuthLogin(
		@PathVariable OAuthProviderCode provider,
		@Valid @RequestBody OAuthCallbackRequest request
	) {
		return notImplemented();
	}

	@GetMapping("/policies")
	public List<PolicyDocument> listPolicies(
		@RequestParam(required = false) String languageCode,
		@RequestParam(required = false) Boolean requiredOnly
	) {
		return notImplemented();
	}

	@GetMapping("/sessions")
	public PagedUserSession listSessions(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}

	@DeleteMapping("/sessions/{sessionId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void revokeSession(@PathVariable UUID sessionId) {
		notImplemented();
	}

	@GetMapping("/security-events")
	public PagedSecurityEvent listSecurityEvents(
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}
}

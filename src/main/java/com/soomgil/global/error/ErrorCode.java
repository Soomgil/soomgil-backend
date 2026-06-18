package com.soomgil.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 실패 응답에서 사용하는 안정적인 error code 목록.
 *
 * <p>각 값은 HTTP status, frontend 분기용 문자열 code, 기본 메시지를 함께 가진다.
 * 도메인별 세부 code가 필요해지기 전까지 공통 code로 실패 의미를 맞춘다.
 */
public enum ErrorCode {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request."),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource was not found."),
	CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with current state."),
	BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", "Business rule violation."),
	NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", "Endpoint is not implemented yet."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error."),
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "Email or password is incorrect."),
	EMAIL_ALREADY_USED(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED", "Email is already registered."),
	USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."),
	USER_INACTIVE(HttpStatus.FORBIDDEN, "USER_INACTIVE", "User account is not active."),
	REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "Refresh token is invalid."),
	REFRESH_TOKEN_REUSE_DETECTED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_REUSE_DETECTED", "Refresh token reuse detected."),
	EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED", "Email address is not verified."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "Token is invalid."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "Token has expired."),
	OAUTH_PROVIDER_ERROR(HttpStatus.BAD_GATEWAY, "OAUTH_PROVIDER_ERROR", "OAuth provider returned an error."),
	OAUTH_EMAIL_CONFLICT(HttpStatus.CONFLICT, "OAUTH_EMAIL_CONFLICT", "Email is already used by another account."),
	OAUTH_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "OAUTH_NOT_CONFIGURED", "OAuth provider is not configured."),
	POLICY_NOT_ACCEPTED(HttpStatus.FORBIDDEN, "POLICY_NOT_ACCEPTED", "Required policy must be accepted."),
	SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "SESSION_NOT_FOUND", "Session was not found."),
	INVALID_PROFILE_IMAGE(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PROFILE_IMAGE", "Profile image reference is invalid."),
	INVALID_TIMEZONE(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TIMEZONE", "Timezone is not supported."),
	INVALID_DISPLAY_LANGUAGE(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_DISPLAY_LANGUAGE", "Display language is not supported."),
	ACCOUNT_DELETION_BLOCKED_BY_ACTIVE_OWNER_TRIP(HttpStatus.CONFLICT, "ACCOUNT_DELETION_BLOCKED_BY_ACTIVE_OWNER_TRIP", "Active trip ownership blocks account deletion."),
	PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "User profile was not found.");

	private final HttpStatus status;
	private final String code;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String code, String defaultMessage) {
		this.status = status;
		this.code = code;
		this.defaultMessage = defaultMessage;
	}

	/**
	 * 이 error code에 대응하는 HTTP status.
	 *
	 * @return HTTP status
	 */
	public HttpStatus status() {
		return status;
	}

	/**
	 * response body의 {@code code} field에 들어가는 안정적인 문자열.
	 *
	 * @return error code 문자열
	 */
	public String code() {
		return code;
	}

	/**
	 * 별도 상세 메시지가 없을 때 사용하는 기본 설명.
	 *
	 * @return 기본 메시지
	 */
	public String defaultMessage() {
		return defaultMessage;
	}
}

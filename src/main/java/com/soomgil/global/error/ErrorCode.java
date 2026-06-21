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
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNPROCESSABLE_ENTITY, "UNSUPPORTED_MEDIA_TYPE", "Media type is not supported."),
	MEDIA_SIZE_LIMIT_EXCEEDED(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_SIZE_LIMIT_EXCEEDED", "Media size limit was exceeded."),
	OBJECT_NOT_FOUND(HttpStatus.UNPROCESSABLE_ENTITY, "OBJECT_NOT_FOUND", "Uploaded object was not found."),
	MEDIA_METADATA_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "MEDIA_METADATA_MISMATCH", "Media metadata does not match the uploaded object."),
	MEDIA_LINK_FORBIDDEN(HttpStatus.FORBIDDEN, "MEDIA_LINK_FORBIDDEN", "Media cannot be linked to the resource."),
	MEDIA_OWNER_REQUIRED(HttpStatus.FORBIDDEN, "MEDIA_OWNER_REQUIRED", "Media owner permission is required."),
	CANNOT_FOLLOW_SELF(HttpStatus.UNPROCESSABLE_ENTITY, "CANNOT_FOLLOW_SELF", "Users cannot follow themselves."),
	FOLLOW_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW_NOT_FOUND", "Follow was not found."),
	FOLLOW_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "FOLLOW_REQUEST_NOT_FOUND", "Follow request was not found."),
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
	PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "PROFILE_NOT_FOUND", "User profile was not found."),
	POST_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_NOT_FOUND", "Community post was not found."),
	POST_AUTHOR_REQUIRED(HttpStatus.FORBIDDEN, "POST_AUTHOR_REQUIRED", "Only the post author can perform this action."),
	INVALID_SHARE_TOKEN(HttpStatus.FORBIDDEN, "INVALID_SHARE_TOKEN", "Share token is invalid or missing."),
	TRIP_MEMBER_REQUIRED(HttpStatus.FORBIDDEN, "TRIP_MEMBER_REQUIRED", "Only active trip members can publish."),
	SOURCE_TRIP_VERSION_CONFLICT(HttpStatus.CONFLICT, "SOURCE_TRIP_VERSION_CONFLICT", "Trip version has changed since last read."),
	REPORT_TARGET_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_TARGET_NOT_FOUND", "Reported content was not found."),
	DUPLICATE_REPORT(HttpStatus.CONFLICT, "DUPLICATE_REPORT", "Already reported this content."),
	CANNOT_REPORT_OWN_CONTENT(HttpStatus.UNPROCESSABLE_ENTITY, "CANNOT_REPORT_OWN_CONTENT", "Cannot report your own content."),
	REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "REPORT_NOT_FOUND", "Report was not found."),
	INVALID_REPORT_TRANSITION(HttpStatus.CONFLICT, "INVALID_REPORT_TRANSITION", "Report cannot transition from its current status."),
	MODERATION_ACCESS_DENIED(HttpStatus.FORBIDDEN, "MODERATION_ACCESS_DENIED", "Moderator role is required."),
	PLANNING_NOTE_NOT_FOUND(HttpStatus.NOT_FOUND, "PLANNING_NOTE_NOT_FOUND", "Planning note was not found."),
	PLANNING_CHECKLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "PLANNING_CHECKLIST_NOT_FOUND", "Planning checklist was not found."),
	PLANNING_ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "PLANNING_ITEM_NOT_FOUND", "Planning checklist item was not found."),
	PLANNING_VERSION_CONFLICT(HttpStatus.CONFLICT, "PLANNING_VERSION_CONFLICT", "Planning resource version has changed since last read."),
	PLANNING_SCOPE_DAY_MISMATCH(HttpStatus.UNPROCESSABLE_ENTITY, "PLANNING_SCOPE_DAY_MISMATCH", "Scope type and itinerary day id are inconsistent.");

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

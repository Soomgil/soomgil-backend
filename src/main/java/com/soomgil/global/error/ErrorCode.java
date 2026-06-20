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
	NOT_IMPLEMENTED(HttpStatus.NOT_IMPLEMENTED, "NOT_IMPLEMENTED", "Endpoint is not implemented yet."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Unexpected server error.");

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

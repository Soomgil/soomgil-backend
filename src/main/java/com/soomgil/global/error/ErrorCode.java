package com.soomgil.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
	INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request."),
	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Request validation failed."),
	UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Resource was not found."),
	CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "Request conflicts with current state."),
	BUSINESS_RULE_VIOLATION(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", "Business rule violation."),
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

	public HttpStatus status() {
		return status;
	}

	public String code() {
		return code;
	}

	public String defaultMessage() {
		return defaultMessage;
	}
}

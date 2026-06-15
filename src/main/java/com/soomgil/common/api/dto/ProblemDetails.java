package com.soomgil.common.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * API 실패 응답의 공통 body.
 *
 * <p>RFC Problem Details 형식을 따르되, frontend 분기 처리를 위해 안정적인 {@code code},
 * 요청 추적을 위한 {@code requestId}, field validation 정보를 함께 담는다.
 * {@code instance}는 요청 path이고, {@code method}는 HTTP method이다.
 */
public record ProblemDetails(
	@NotNull
	URI type,
	@NotBlank
	String title,
	@NotNull
	Integer status,
	String detail,
	String instance,
	String method,
	@NotBlank
	String code,
	String requestId,
	@Valid
	List<ProblemField> fields
) {

	public ProblemDetails {
		if (fields == null) {
			fields = List.of();
		}
		else {
			fields = List.copyOf(new ArrayList<>(fields));
		}
	}
}

package com.soomgil.common.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

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

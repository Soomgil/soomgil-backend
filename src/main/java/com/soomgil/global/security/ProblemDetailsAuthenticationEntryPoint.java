package com.soomgil.global.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.ProblemDetails;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.error.ProblemDetailsFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * Spring Security 인증 실패를 401 ProblemDetails 응답으로 변환한다.
 *
 * <p>토큰이 없거나 잘못되어 현재 사용자를 식별할 수 없는 요청에 사용된다.
 * 로그인은 되었지만 권한이 부족한 경우는 {@link ProblemDetailsAccessDeniedHandler}가 403으로 처리한다.
 */
@Component
public class ProblemDetailsAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ProblemDetailsFactory problemDetailsFactory;
	private final ObjectMapper objectMapper;

	public ProblemDetailsAuthenticationEntryPoint(
		ProblemDetailsFactory problemDetailsFactory,
		ObjectMapper objectMapper
	) {
		this.problemDetailsFactory = problemDetailsFactory;
		this.objectMapper = objectMapper;
	}

	@Override
	public void commence(
		HttpServletRequest request,
		HttpServletResponse response,
		AuthenticationException authException
	) throws IOException {
		ProblemDetails problemDetails = problemDetailsFactory.create(
			ErrorCode.UNAUTHORIZED,
			ErrorCode.UNAUTHORIZED.defaultMessage(),
			request,
			List.of()
		);

		response.setStatus(ErrorCode.UNAUTHORIZED.status().value());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		objectMapper.writeValue(response.getOutputStream(), problemDetails);
	}
}

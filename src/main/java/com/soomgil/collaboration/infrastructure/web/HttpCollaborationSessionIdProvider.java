package com.soomgil.collaboration.infrastructure.web;

import com.soomgil.collaboration.application.port.CollaborationSessionIdProvider;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 현재 HTTP 요청 헤더에서 협업 WebSocket session ID를 읽는다.
 */
@Component
public class HttpCollaborationSessionIdProvider implements CollaborationSessionIdProvider {

	public static final String SESSION_HEADER = "X-Soomgil-WebSocket-Session-Id";

	@Override
	public String currentSessionId() {
		if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
			return null;
		}
		HttpServletRequest request = attributes.getRequest();
		String value = request.getHeader(SESSION_HEADER);
		return value == null || value.isBlank() ? null : value.trim();
	}
}

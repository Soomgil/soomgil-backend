package com.soomgil.community.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.CreateContentReportRequest;
import com.soomgil.community.api.dto.ReportReason;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.application.command.CreateContentReportCommand;
import com.soomgil.community.application.handler.CreateContentReportCommandHandler;
import com.soomgil.community.application.handler.ListReportReasonsQueryHandler;
import com.soomgil.community.application.query.ListReportReasonsQuery;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = CommunityReportController.class)
@Import({
	CommunityReportControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class CommunityReportControllerWebMvcTest {

	private static final UUID AUTHENTICATED_USER_ID = UUID.randomUUID();
	private static final CurrentUser AUTHENTICATED_USER =
		new CurrentUser(AUTHENTICATED_USER_ID, "reporter@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ListReportReasonsQueryHandler listReportReasonsQueryHandler;
	@MockBean
	private CreateContentReportCommandHandler createContentReportCommandHandler;

	static RequestPostProcessor asCurrentUser() {
		var authentication =
			new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				AUTHENTICATED_USER, null, List.of()
			);
		return SecurityMockMvcRequestPostProcessors.authentication(authentication);
	}

	@Test
	@DisplayName("GET /community/reports/reasons - 비로그인도 신고 사유 목록을 200으로 조회할 수 있다")
	void listReasonsReturns200WithoutAuth() throws Exception {
		when(listReportReasonsQueryHandler.handle(any(ListReportReasonsQuery.class)))
			.thenReturn(List.of(
				new ReportReason(ReportReasonCode.SPAM, "스팸 · 광고", true)
			));

		mockMvc.perform(get("/api/v1/community/reports/reasons"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].code").value("SPAM"));
	}

	@Test
	@DisplayName("POST /community/reports - 인증된 사용자는 신고할 수 있다 (201)")
	void createReportReturns201WithAuth() throws Exception {
		UUID targetId = UUID.randomUUID();
		when(createContentReportCommandHandler.handle(any(CreateContentReportCommand.class)))
			.thenReturn(new ContentReport(
				UUID.randomUUID(), null, ReportTargetType.POST, targetId,
				ReportReasonCode.SPAM, "spam post", ReportStatus.OPEN,
				OffsetDateTime.now(), null, null
			));

		CreateContentReportRequest body = new CreateContentReportRequest(
			ReportTargetType.POST, targetId, ReportReasonCode.SPAM, "spam post"
		);

		mockMvc.perform(post("/api/v1/community/reports")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.status").value("OPEN"));
	}

	@Test
	@DisplayName("POST /community/reports - 비로그인 신고 시도는 401을 반환한다")
	void createReportReturns401WithoutAuth() throws Exception {
		CreateContentReportRequest body = new CreateContentReportRequest(
			ReportTargetType.POST, UUID.randomUUID(), ReportReasonCode.SPAM, "spam"
		);

		mockMvc.perform(post("/api/v1/community/reports")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isUnauthorized());
	}

	@TestConfiguration
	static class TestSecurityConfig {
		@Bean
		SecurityFilterChain testSecurityFilterChain(
			HttpSecurity http,
			ProblemDetailsAuthenticationEntryPoint authenticationEntryPoint,
			ProblemDetailsAccessDeniedHandler accessDeniedHandler
		) throws Exception {
			return http
				.csrf(AbstractHttpConfigurer::disable)
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.exceptionHandling(exception -> exception
					.authenticationEntryPoint(authenticationEntryPoint)
					.accessDeniedHandler(accessDeniedHandler))
				.authorizeHttpRequests(auth -> auth
					.requestMatchers(HttpMethod.GET, "/api/v1/community/reports/reasons").permitAll()
					.anyRequest().authenticated())
				.build();
		}
	}
}

package com.soomgil.community.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.community.api.dto.ContentReport;
import com.soomgil.community.api.dto.CreateModerationActionRequest;
import com.soomgil.community.api.dto.ModerationAction;
import com.soomgil.community.api.dto.ModerationActionType;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PagedContentReport;
import com.soomgil.community.api.dto.PagedModerationAction;
import com.soomgil.community.api.dto.ReportReasonCode;
import com.soomgil.community.api.dto.ReportStatus;
import com.soomgil.community.api.dto.ReportTargetType;
import com.soomgil.community.api.dto.ResolveReportRequest;
import com.soomgil.community.application.command.CreateModerationActionCommand;
import com.soomgil.community.application.command.ResolveReportCommand;
import com.soomgil.community.application.handler.CreateModerationActionCommandHandler;
import com.soomgil.community.application.handler.ListModerationActionsQueryHandler;
import com.soomgil.community.application.handler.ListReportsQueryHandler;
import com.soomgil.community.application.handler.ResolveReportCommandHandler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.global.error.ErrorCode;
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
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(controllers = ModerationController.class)
@Import({
	ModerationControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class ModerationControllerWebMvcTest {

	private static final UUID MODERATOR_ID = UUID.randomUUID();
	private static final CurrentUser MODERATOR_USER =
		new CurrentUser(MODERATOR_ID, "moderator@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ListReportsQueryHandler listReportsQueryHandler;
	@MockBean
	private ResolveReportCommandHandler resolveReportCommandHandler;
	@MockBean
	private ListModerationActionsQueryHandler listModerationActionsQueryHandler;
	@MockBean
	private CreateModerationActionCommandHandler createModerationActionCommandHandler;

	static RequestPostProcessor asModerator() {
		var authentication =
			new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				MODERATOR_USER, null, List.of()
			);
		return SecurityMockMvcRequestPostProcessors.authentication(authentication);
	}

	@Test
	@DisplayName("GET /moderation/reports - 모더레이터는 신고 목록을 200으로 조회할 수 있다")
	void listReportsReturns200ForModerator() throws Exception {
		when(listReportsQueryHandler.handle(any()))
			.thenReturn(new PagedContentReport(List.of(),
				new PageMeta(0, 20, 0L, 0, List.of())));

		mockMvc.perform(get("/api/v1/moderation/reports")
				.with(asModerator()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").exists());
	}

	@Test
	@DisplayName("GET /moderation/reports - 비로그인은 401을 반환한다")
	void listReportsReturns401WithoutAuth() throws Exception {
		mockMvc.perform(get("/api/v1/moderation/reports"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("PATCH /moderation/reports/{id} - 모더레이터는 신고를 처리할 수 있다 (200)")
	void resolveReportReturns200ForModerator() throws Exception {
		UUID reportId = UUID.randomUUID();
		when(resolveReportCommandHandler.handle(any(ResolveReportCommand.class)))
			.thenReturn(new ContentReport(
				reportId, null, ReportTargetType.POST, UUID.randomUUID(),
				ReportReasonCode.SPAM, "spam", ReportStatus.RESOLVED,
				OffsetDateTime.now(), OffsetDateTime.now(), "resolved"
			));

		ResolveReportRequest body = new ResolveReportRequest("RESOLVED", "resolved", null);

		mockMvc.perform(patch("/api/v1/moderation/reports/" + reportId)
				.with(asModerator())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("RESOLVED"));
	}

	@Test
	@DisplayName("POST /moderation/actions - 모더레이터는 조치를 생성할 수 있다 (201)")
	void createActionReturns201ForModerator() throws Exception {
		UUID targetId = UUID.randomUUID();
		when(createModerationActionCommandHandler.handle(any(CreateModerationActionCommand.class)))
			.thenReturn(new ModerationAction(
				UUID.randomUUID(), null, ReportTargetType.POST, targetId,
				ModerationActionType.HIDE, ModerationStatus.HIDDEN, "spam",
				OffsetDateTime.now()
			));

		CreateModerationActionRequest body = new CreateModerationActionRequest(
			ReportTargetType.POST, targetId, ModerationActionType.HIDE, "spam"
		);

		mockMvc.perform(post("/api/v1/moderation/actions")
				.with(asModerator())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.action").value("HIDE"));
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
					.anyRequest().authenticated())
				.build();
		}
	}
}

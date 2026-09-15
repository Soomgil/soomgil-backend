package com.soomgil.community.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.community.api.dto.CommunityThread;
import com.soomgil.community.api.dto.CommunityThreadReactionSummary;
import com.soomgil.community.api.dto.CommunityThreadReply;
import com.soomgil.community.api.dto.CreateCommunityThreadReplyRequest;
import com.soomgil.community.api.dto.CreateCommunityThreadRequest;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PagedCommunityThread;
import com.soomgil.community.api.dto.PagedCommunityThreadReply;
import com.soomgil.community.api.dto.UpdateCommunityThreadRequest;
import com.soomgil.community.application.command.CreateCommunityThreadCommand;
import com.soomgil.community.application.command.DeleteCommunityThreadCommand;
import com.soomgil.community.application.command.UpdateCommunityThreadCommand;
import com.soomgil.community.application.handler.CreateCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.CreateCommunityThreadReplyCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityThreadReplyCommandHandler;
import com.soomgil.community.application.handler.GetCommunityThreadQueryHandler;
import com.soomgil.community.application.handler.LikeCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.ListCommunityThreadRepliesQueryHandler;
import com.soomgil.community.application.handler.ListCommunityThreadsQueryHandler;
import com.soomgil.community.application.handler.UnlikeCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.UpdateCommunityThreadCommandHandler;
import com.soomgil.community.application.handler.UpdateCommunityThreadReplyCommandHandler;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import com.soomgil.user.api.dto.UserSummary;
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

@WebMvcTest(controllers = CommunityThreadController.class)
@Import({
	CommunityThreadControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class CommunityThreadControllerWebMvcTest {

	private static final UUID AUTHENTICATED_USER_ID = UUID.randomUUID();
	private static final CurrentUser AUTHENTICATED_USER =
		new CurrentUser(AUTHENTICATED_USER_ID, "author@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CreateCommunityThreadCommandHandler createCommunityThreadCommandHandler;
	@MockBean
	private UpdateCommunityThreadCommandHandler updateCommunityThreadCommandHandler;
	@MockBean
	private DeleteCommunityThreadCommandHandler deleteCommunityThreadCommandHandler;
	@MockBean
	private LikeCommunityThreadCommandHandler likeCommunityThreadCommandHandler;
	@MockBean
	private UnlikeCommunityThreadCommandHandler unlikeCommunityThreadCommandHandler;
	@MockBean
	private CreateCommunityThreadReplyCommandHandler createCommunityThreadReplyCommandHandler;
	@MockBean
	private UpdateCommunityThreadReplyCommandHandler updateCommunityThreadReplyCommandHandler;
	@MockBean
	private DeleteCommunityThreadReplyCommandHandler deleteCommunityThreadReplyCommandHandler;
	@MockBean
	private ListCommunityThreadsQueryHandler listCommunityThreadsQueryHandler;
	@MockBean
	private GetCommunityThreadQueryHandler getCommunityThreadQueryHandler;
	@MockBean
	private ListCommunityThreadRepliesQueryHandler listCommunityThreadRepliesQueryHandler;

	@Test
	@DisplayName("공개 피드 조회는 인증 없이 200으로 응답한다")
	void publicFeedIsAccessibleWithoutAuthentication() throws Exception {
		when(listCommunityThreadsQueryHandler.handle(any())).thenReturn(new PagedCommunityThread(
			List.of(sampleThread()),
			new PageMeta(0, 20, 1L, 1, List.of("createdAt,desc", "id,desc"))
		));

		mockMvc.perform(get("/api/v1/community/threads"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].content").value("성심당 줄이 미쳤어요"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	@DisplayName("쓰레드 상세와 답글 목록도 공개 조회가 가능하다")
	void publicDetailAndRepliesAreAccessible() throws Exception {
		UUID threadId = UUID.randomUUID();
		when(getCommunityThreadQueryHandler.handle(any())).thenReturn(sampleThread());
		when(listCommunityThreadRepliesQueryHandler.handle(any())).thenReturn(new PagedCommunityThreadReply(
			List.of(sampleReply(threadId)),
			new PageMeta(0, 20, 1L, 1, List.of("createdAt,asc", "id,asc"))
		));

		mockMvc.perform(get("/api/v1/community/threads/{threadId}", threadId))
			.andExpect(status().isOk());
		mockMvc.perform(get("/api/v1/community/threads/{threadId}/replies", threadId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].depth").value(0));
	}

	@Test
	@DisplayName("쓰레드 작성은 인증이 필요하고 201을 반환한다")
	void createThreadRequiresAuthentication() throws Exception {
		when(createCommunityThreadCommandHandler.handle(any())).thenReturn(sampleThread());
		String body = objectMapper.writeValueAsString(
			new CreateCommunityThreadRequest("성심당 줄이 미쳤어요", List.of())
		);

		mockMvc.perform(post("/api/v1/community/threads")
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnauthorized());

		mockMvc.perform(post("/api/v1/community/threads")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated());

		verify(createCommunityThreadCommandHandler).handle(any(CreateCommunityThreadCommand.class));
	}

	@Test
	@DisplayName("빈 본문 요청은 400 ProblemDetails로 거부한다")
	void blankContentIsRejectedByValidation() throws Exception {
		String body = objectMapper.writeValueAsString(new CreateCommunityThreadRequest("   ", List.of()));

		mockMvc.perform(post("/api/v1/community/threads")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("작성자가 아닌 사용자의 수정 요청은 403 ProblemDetails로 응답한다")
	void nonAuthorUpdateReturnsForbiddenProblemDetails() throws Exception {
		when(updateCommunityThreadCommandHandler.handle(any(UpdateCommunityThreadCommand.class)))
			.thenThrow(new CommunityException(ErrorCode.THREAD_AUTHOR_REQUIRED));
		String body = objectMapper.writeValueAsString(
			new UpdateCommunityThreadRequest("수정 시도", null)
		);

		mockMvc.perform(patch("/api/v1/community/threads/{threadId}", UUID.randomUUID())
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isForbidden())
			.andExpect(jsonPath("$.code").value("THREAD_AUTHOR_REQUIRED"));
	}

	@Test
	@DisplayName("없는 쓰레드 조회는 404 ProblemDetails로 응답한다")
	void missingThreadReturnsNotFoundProblemDetails() throws Exception {
		when(getCommunityThreadQueryHandler.handle(any()))
			.thenThrow(new CommunityException(ErrorCode.THREAD_NOT_FOUND));

		mockMvc.perform(get("/api/v1/community/threads/{threadId}", UUID.randomUUID()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("THREAD_NOT_FOUND"));
	}

	@Test
	@DisplayName("삭제는 204를 반환한다")
	void deleteReturnsNoContent() throws Exception {
		mockMvc.perform(delete("/api/v1/community/threads/{threadId}", UUID.randomUUID())
				.with(asCurrentUser()))
			.andExpect(status().isNoContent());

		verify(deleteCommunityThreadCommandHandler).handle(any(DeleteCommunityThreadCommand.class));
	}

	@Test
	@DisplayName("좋아요는 PUT, 취소는 DELETE로 처리하고 요약을 반환한다")
	void likeAndUnlikeUseIdempotentVerbs() throws Exception {
		UUID threadId = UUID.randomUUID();
		when(likeCommunityThreadCommandHandler.handle(any()))
			.thenReturn(new CommunityThreadReactionSummary(threadId, true, 4));
		when(unlikeCommunityThreadCommandHandler.handle(any()))
			.thenReturn(new CommunityThreadReactionSummary(threadId, false, 3));

		mockMvc.perform(put("/api/v1/community/threads/{threadId}/like", threadId).with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(true))
			.andExpect(jsonPath("$.likeCount").value(4));

		mockMvc.perform(delete("/api/v1/community/threads/{threadId}/like", threadId).with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(false))
			.andExpect(jsonPath("$.likeCount").value(3));
	}

	@Test
	@DisplayName("2단계 중첩 답글 시도는 422 ProblemDetails로 응답한다")
	void nestedReplyDepthViolationReturnsUnprocessable() throws Exception {
		UUID threadId = UUID.randomUUID();
		when(createCommunityThreadReplyCommandHandler.handle(any()))
			.thenThrow(new CommunityException(ErrorCode.THREAD_REPLY_DEPTH_EXCEEDED));
		String body = objectMapper.writeValueAsString(
			new CreateCommunityThreadReplyRequest(UUID.randomUUID(), "더 깊은 답글")
		);

		mockMvc.perform(post("/api/v1/community/threads/{threadId}/replies", threadId)
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("THREAD_REPLY_DEPTH_EXCEEDED"));
	}

	@Test
	@DisplayName("답글 작성은 201, 답글 삭제는 204를 반환한다")
	void replyCreateAndDeleteStatusCodes() throws Exception {
		UUID threadId = UUID.randomUUID();
		when(createCommunityThreadReplyCommandHandler.handle(any())).thenReturn(sampleReply(threadId));
		String body = objectMapper.writeValueAsString(
			new CreateCommunityThreadReplyRequest(null, "저도 갔어요")
		);

		mockMvc.perform(post("/api/v1/community/threads/{threadId}/replies", threadId)
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(body))
			.andExpect(status().isCreated());

		mockMvc.perform(delete("/api/v1/community/threads/{threadId}/replies/{replyId}",
				threadId, UUID.randomUUID()).with(asCurrentUser()))
			.andExpect(status().isNoContent());
	}

	private CommunityThread sampleThread() {
		return new CommunityThread(
			UUID.randomUUID(),
			new UserSummary(AUTHENTICATED_USER_ID, "소윤", null),
			"성심당 줄이 미쳤어요",
			List.of(),
			4,
			1,
			false,
			true,
			ModerationStatus.VISIBLE,
			null,
			OffsetDateTime.parse("2026-08-24T00:00:00Z"),
			null
		);
	}

	private CommunityThreadReply sampleReply(UUID threadId) {
		return new CommunityThreadReply(
			UUID.randomUUID(),
			threadId,
			null,
			new UserSummary(AUTHENTICATED_USER_ID, "소윤", null),
			"저도 갔어요",
			0,
			true,
			ModerationStatus.VISIBLE,
			null,
			OffsetDateTime.parse("2026-08-24T00:00:00Z"),
			null,
			List.of()
		);
	}

	static RequestPostProcessor asCurrentUser() {
		var authentication =
			new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				AUTHENTICATED_USER, null, List.of()
			);
		return SecurityMockMvcRequestPostProcessors.authentication(authentication);
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
					.requestMatchers(HttpMethod.GET,
						"/api/v1/community/threads", "/api/v1/community/threads/*",
						"/api/v1/community/threads/*/replies").permitAll()
					.anyRequest().authenticated())
				.build();
		}
	}
}

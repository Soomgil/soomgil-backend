package com.soomgil.community.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.community.api.dto.CommunityComment;
import com.soomgil.community.api.dto.CommunityPostDetail;
import com.soomgil.community.api.dto.CommunityPostReactionSummary;
import com.soomgil.community.api.dto.CommunityPostShareTokenResponse;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.api.dto.CreateCommunityCommentRequest;
import com.soomgil.community.api.dto.CreateCommunityPostRequest;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PagedCommunityComment;
import com.soomgil.community.api.dto.PagedCommunityPostSummary;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.api.dto.UpdateCommunityPostRequest;
import com.soomgil.community.application.command.CreateCommunityCommentCommand;
import com.soomgil.community.application.command.CreateCommunityPostCommand;
import com.soomgil.community.application.command.DeleteCommunityCommentCommand;
import com.soomgil.community.application.command.DeleteCommunityPostCommand;
import com.soomgil.community.application.command.LikePostCommand;
import com.soomgil.community.application.command.RotatePostShareTokenCommand;
import com.soomgil.community.application.command.UnlikePostCommand;
import com.soomgil.community.application.command.UpdateCommunityPostCommand;
import com.soomgil.community.application.handler.CreateCommunityCommentCommandHandler;
import com.soomgil.community.application.handler.CreateCommunityPostCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityCommentCommandHandler;
import com.soomgil.community.application.handler.DeleteCommunityPostCommandHandler;
import com.soomgil.community.application.handler.GetCommunityPostQueryHandler;
import com.soomgil.community.application.handler.LikePostCommandHandler;
import com.soomgil.community.application.handler.ListCommentsQueryHandler;
import com.soomgil.community.application.handler.ListCommunityPostsQueryHandler;
import com.soomgil.community.application.handler.RotatePostShareTokenCommandHandler;
import com.soomgil.community.application.handler.UnlikePostCommandHandler;
import com.soomgil.community.application.handler.UpdateCommunityPostCommandHandler;
import com.soomgil.community.application.service.RetripCommunityPostService;
import com.soomgil.community.application.query.GetCommunityPostQuery;
import com.soomgil.community.application.query.ListCommentsQuery;
import com.soomgil.community.application.query.ListCommunityPostsQuery;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import java.net.URI;
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

@WebMvcTest(controllers = CommunityPostController.class)
@Import({
	CommunityPostControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class CommunityPostControllerWebMvcTest {

	private static final UUID AUTHENTICATED_USER_ID = UUID.randomUUID();
	private static final CurrentUser AUTHENTICATED_USER =
		new CurrentUser(AUTHENTICATED_USER_ID, "author@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private CreateCommunityPostCommandHandler createCommunityPostCommandHandler;
	@MockBean
	private UpdateCommunityPostCommandHandler updateCommunityPostCommandHandler;
	@MockBean
	private DeleteCommunityPostCommandHandler deleteCommunityPostCommandHandler;
	@MockBean
	private RotatePostShareTokenCommandHandler rotatePostShareTokenCommandHandler;
	@MockBean
	private GetCommunityPostQueryHandler getCommunityPostQueryHandler;
	@MockBean
	private ListCommunityPostsQueryHandler listCommunityPostsQueryHandler;
	@MockBean
	private LikePostCommandHandler likePostCommandHandler;
	@MockBean
	private UnlikePostCommandHandler unlikePostCommandHandler;
	@MockBean
	private CreateCommunityCommentCommandHandler createCommunityCommentCommandHandler;
	@MockBean
	private DeleteCommunityCommentCommandHandler deleteCommunityCommentCommandHandler;
	@MockBean
	private ListCommentsQueryHandler listCommentsQueryHandler;
	@MockBean
	private RetripCommunityPostService retripCommunityPostService;

	static RequestPostProcessor asCurrentUser() {
		var authentication =
			new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				AUTHENTICATED_USER, null, List.of()
			);
		return SecurityMockMvcRequestPostProcessors.authentication(authentication);
	}

	// ---- Phase 1: Posts CRUD ----

	@Test
	@DisplayName("GET /community/posts - 비로그인도 공개 feed를 200으로 조회할 수 있다")
	void listPostsReturns200WithoutAuth() throws Exception {
		when(listCommunityPostsQueryHandler.handle(any(ListCommunityPostsQuery.class)))
			.thenReturn(new PagedCommunityPostSummary(List.of(),
				new PageMeta(0, 20, 0L, 0, List.of())));

		mockMvc.perform(get("/api/v1/community/posts"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").exists())
			.andExpect(jsonPath("$.page").exists());
	}

	@Test
	@DisplayName("GET /community/posts/{postId} - 비로그인도 공개 게시글을 200으로 조회할 수 있다")
	void getPostReturns200WithoutAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(getCommunityPostQueryHandler.handle(any(GetCommunityPostQuery.class)))
			.thenReturn(sampleDetail(postId));

		mockMvc.perform(get("/api/v1/community/posts/" + postId))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(postId.toString()));
	}

	@Test
	@DisplayName("POST /community/posts - 인증된 사용자는 게시글을 발행할 수 있다 (201)")
	void createPostReturns201WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(createCommunityPostCommandHandler.handle(any(CreateCommunityPostCommand.class)))
			.thenReturn(sampleDetail(postId));

		CreateCommunityPostRequest body = new CreateCommunityPostRequest(
			UUID.randomUUID(), 1L, PostVisibility.PUBLIC, "제주도 여행", null, null, null, null
		);

		mockMvc.perform(post("/api/v1/community/posts")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").value(postId.toString()));
	}

	@Test
	@DisplayName("POST /community/posts - 비로그인 발행 시도는 401을 반환한다")
	void createPostReturns401WithoutAuth() throws Exception {
		CreateCommunityPostRequest body = new CreateCommunityPostRequest(
			UUID.randomUUID(), 1L, PostVisibility.PUBLIC, "제주도 여행", null, null, null, null
		);

		mockMvc.perform(post("/api/v1/community/posts")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("PATCH /community/posts/{postId} - 인증된 사용자는 게시글을 수정할 수 있다")
	void updatePostReturns200WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(updateCommunityPostCommandHandler.handle(any(UpdateCommunityPostCommand.class)))
			.thenReturn(sampleDetail(postId));

		UpdateCommunityPostRequest body = new UpdateCommunityPostRequest(
			null, "수정된 제목", null, null, null, null
		);

		mockMvc.perform(patch("/api/v1/community/posts/" + postId)
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk());
	}

	@Test
	@DisplayName("DELETE /community/posts/{postId} - 인증된 발행자는 204로 삭제할 수 있다")
	void deletePostReturns204WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(deleteCommunityPostCommandHandler.handle(any(DeleteCommunityPostCommand.class)))
			.thenReturn(NoResult.INSTANCE);

		mockMvc.perform(delete("/api/v1/community/posts/" + postId)
				.with(asCurrentUser()))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("POST /community/posts/{postId}/share-token - 인증된 발행자는 공유 토큰을 rotate할 수 있다")
	void rotateShareTokenReturns200WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(rotatePostShareTokenCommandHandler.handle(any(RotatePostShareTokenCommand.class)))
			.thenReturn(new CommunityPostShareTokenResponse(
				postId, "new-raw-token", URI.create("https://soomgil.example.com/s/" + postId),
				OffsetDateTime.now()
			));

		mockMvc.perform(post("/api/v1/community/posts/" + postId + "/share-token")
				.with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.shareToken").value("new-raw-token"));
	}

	// ---- Phase 2: Likes ----

	@Test
	@DisplayName("POST /community/posts/{postId}/likes - 인증된 사용자는 좋아요할 수 있다 (200)")
	void likePostReturns200WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(likePostCommandHandler.handle(any(LikePostCommand.class)))
			.thenReturn(new CommunityPostReactionSummary(postId, true, 1));

		mockMvc.perform(post("/api/v1/community/posts/" + postId + "/likes")
				.with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(true))
			.andExpect(jsonPath("$.likeCount").value(1));
	}

	@Test
	@DisplayName("DELETE /community/posts/{postId}/likes - 인증된 사용자는 좋아요를 취소할 수 있다 (200)")
	void unlikePostReturns200WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(unlikePostCommandHandler.handle(any(UnlikePostCommand.class)))
			.thenReturn(new CommunityPostReactionSummary(postId, false, 0));

		mockMvc.perform(delete("/api/v1/community/posts/" + postId + "/likes")
				.with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.liked").value(false));
	}

	@Test
	@DisplayName("POST /community/posts/{postId}/likes - 비로그인 좋아요는 401을 반환한다")
	void likePostReturns401WithoutAuth() throws Exception {
		mockMvc.perform(post("/api/v1/community/posts/" + UUID.randomUUID() + "/likes"))
			.andExpect(status().isUnauthorized());
	}

	// ---- Phase 2: Comments ----

	@Test
	@DisplayName("GET /community/posts/{postId}/comments - 비로그인도 댓글 목록을 200으로 조회할 수 있다")
	void listCommentsReturns200WithoutAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(listCommentsQueryHandler.handle(any(ListCommentsQuery.class)))
			.thenReturn(new PagedCommunityComment(List.of(),
				new PageMeta(0, 20, 0L, 0, List.of())));

		mockMvc.perform(get("/api/v1/community/posts/" + postId + "/comments"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").exists());
	}

	@Test
	@DisplayName("POST /community/posts/{postId}/comments - 인증된 사용자는 댓글을 작성할 수 있다 (201)")
	void createCommentReturns201WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		when(createCommunityCommentCommandHandler.handle(any(CreateCommunityCommentCommand.class)))
			.thenReturn(new CommunityComment(
				UUID.randomUUID(), postId, null, null, "멋진 여행이네요!",
				0, ModerationStatus.VISIBLE, null, OffsetDateTime.now()
			));

		CreateCommunityCommentRequest body = new CreateCommunityCommentRequest(null, "멋진 여행이네요!");

		mockMvc.perform(post("/api/v1/community/posts/" + postId + "/comments")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.content").value("멋진 여행이네요!"));
	}

	@Test
	@DisplayName("DELETE /community/posts/{postId}/comments/{commentId} - 댓글 작성자는 204로 삭제할 수 있다")
	void deleteCommentReturns204WithAuth() throws Exception {
		UUID postId = UUID.randomUUID();
		UUID commentId = UUID.randomUUID();
		when(deleteCommunityCommentCommandHandler.handle(any(DeleteCommunityCommentCommand.class)))
			.thenReturn(NoResult.INSTANCE);

		mockMvc.perform(delete("/api/v1/community/posts/" + postId + "/comments/" + commentId)
				.with(asCurrentUser()))
			.andExpect(status().isNoContent());
	}

	// ---- Fixtures ----

	private CommunityPostDetail sampleDetail(UUID postId) {
		return new CommunityPostDetail(
			postId, UUID.randomUUID(), null, null,
			PostVisibility.PUBLIC, "title", "summary",
			List.of(), 0, 0, 0, 0, false,
			ModerationStatus.VISIBLE, OffsetDateTime.now(), 1,
			new CommunityPostSnapshot(List.of(), List.of(), List.of(), List.of(), null),
			List.of(), null, null, null, null
		);
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
						"/api/v1/community/posts", "/api/v1/community/posts/*",
						"/api/v1/community/posts/*/comments").permitAll()
					.anyRequest().authenticated())
				.build();
		}
	}
}

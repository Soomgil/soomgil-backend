package com.soomgil.user.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.auth.application.handler.GetCurrentUserQueryHandler;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.api.dto.UpdateMeRequest;
import com.soomgil.user.api.dto.UpdateUserSettingsRequest;
import com.soomgil.user.api.dto.User;
import com.soomgil.user.api.dto.UserProfile;
import com.soomgil.user.api.dto.UserProfileVisibility;
import com.soomgil.user.api.dto.UserPublicProfile;
import com.soomgil.user.api.dto.UserSettings;
import com.soomgil.user.api.dto.UserStatus;
import com.soomgil.user.api.dto.UserSummary;
import com.soomgil.user.application.command.RequestAccountDeletionCommand;
import com.soomgil.user.application.command.UpdateMeCommand;
import com.soomgil.user.application.command.UpdateMySettingsCommand;
import com.soomgil.user.application.handler.GetMySettingsQueryHandler;
import com.soomgil.user.application.handler.GetUserPublicProfileQueryHandler;
import com.soomgil.user.application.handler.RequestAccountDeletionCommandHandler;
import com.soomgil.user.application.handler.SearchUsersQueryHandler;
import com.soomgil.user.application.handler.UpdateMeCommandHandler;
import com.soomgil.user.application.handler.UpdateMySettingsCommandHandler;
import com.soomgil.user.application.query.GetUserPublicProfileQuery;
import com.soomgil.user.application.query.SearchUsersQuery;
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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * {@link UserController} 웹 계층 슬라이스 테스트.
 *
 * <p>handler 빈을 Mockito {@link MockBean}으로 교체하여 HTTP 계약과 ProblemDetails 변환을 검증한다.
 * Spring Security는 인증된 {@link CurrentUser}를 주입하는 간단한 테스트 filter chain으로 대체한다.
 */
@WebMvcTest(controllers = UserController.class)
@Import({
	UserControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class UserControllerWebMvcTest {

	private static final UUID AUTHENTICATED_USER_ID = UUID.randomUUID();
	private static final CurrentUser AUTHENTICATED_USER = new CurrentUser(AUTHENTICATED_USER_ID, "viewer@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private GetCurrentUserQueryHandler getCurrentUserQueryHandler;
	@MockBean
	private UpdateMeCommandHandler updateMeCommandHandler;
	@MockBean
	private RequestAccountDeletionCommandHandler requestAccountDeletionCommandHandler;
	@MockBean
	private GetMySettingsQueryHandler getMySettingsQueryHandler;
	@MockBean
	private UpdateMySettingsCommandHandler updateMySettingsCommandHandler;
	@MockBean
	private SearchUsersQueryHandler searchUsersQueryHandler;
	@MockBean
	private GetUserPublicProfileQueryHandler getUserPublicProfileQueryHandler;

	static RequestPostProcessor asCurrentUser() {
		org.springframework.security.authentication.UsernamePasswordAuthenticationToken authentication =
			new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
				AUTHENTICATED_USER, null, java.util.List.of()
			);
		return org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors
			.authentication(authentication);
	}

	@Test
	@DisplayName("PATCH /me - 정상 요청은 200과 갱신된 User를 반환한다")
	void patchMeReturnsUpdatedUser() throws Exception {
		User response = sampleUser();
		when(updateMeCommandHandler.handle(any(UpdateMeCommand.class))).thenReturn(response);

		UpdateMeRequest requestBody = new UpdateMeRequest(
			"민지", null, "updated bio", UserProfileVisibility.PUBLIC
		);

		mockMvc.perform(patch("/api/v1/me")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").exists())
			.andExpect(jsonPath("$.profile.displayName").value("민지"));
	}

	@Test
	@DisplayName("PATCH /me - 인증 없이 호출하면 401 ProblemDetails를 반환한다")
	void patchMeWithoutAuthReturns401() throws Exception {
		UpdateMeRequest requestBody = new UpdateMeRequest("민지", null, null, null);

		mockMvc.perform(patch("/api/v1/me")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
	}

	@Test
	@DisplayName("PATCH /me - display name이 81자면 400 VALIDATION_FAILED를 반환한다")
	void patchMeRejectsTooLongDisplayName() throws Exception {
		UpdateMeRequest requestBody = new UpdateMeRequest(
			"a".repeat(81), null, null, null
		);

		mockMvc.perform(patch("/api/v1/me")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("DELETE /me - 정상 요청은 204 No Content를 반환한다")
	void deleteMeReturns204() throws Exception {
		when(requestAccountDeletionCommandHandler.handle(any(RequestAccountDeletionCommand.class)))
			.thenReturn(NoResult.INSTANCE);

		mockMvc.perform(delete("/api/v1/me").with(asCurrentUser()))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("GET /me/settings - 정상 요청은 200과 UserSettings를 반환한다")
	void getMySettingsReturnsSettings() throws Exception {
		UserSettings settings = new UserSettings(
			"ko", "Asia/Seoul", false, null, null, true
		);
		when(getMySettingsQueryHandler.handle(any(com.soomgil.user.application.query.GetMySettingsQuery.class)))
			.thenReturn(settings);

		mockMvc.perform(get("/api/v1/me/settings").with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayLanguage").value("ko"))
			.andExpect(jsonPath("$.timezone").value("Asia/Seoul"));
	}

	@Test
	@DisplayName("PATCH /me/settings - timezone이 유효하지 않으면 422 INVALID_TIMEZONE을 반환한다")
	void patchMySettingsRejectsInvalidTimezone() throws Exception {
		when(updateMySettingsCommandHandler.handle(any(UpdateMySettingsCommand.class)))
			.thenThrow(new BusinessException(ErrorCode.INVALID_TIMEZONE));

		UpdateUserSettingsRequest requestBody = new UpdateUserSettingsRequest(
			null, "Foo/Bar", null, null
		);

		mockMvc.perform(patch("/api/v1/me/settings")
				.with(asCurrentUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(requestBody)))
			.andExpect(status().isUnprocessableEntity())
			.andExpect(jsonPath("$.code").value("INVALID_TIMEZONE"));
	}

	@Test
	@DisplayName("GET /users - 검색 결과를 PagedUserSummary로 반환한다")
	void searchUsersReturnsPagedResult() throws Exception {
		PagedUserSummary result = new PagedUserSummary(
			List.of(new UserSummary(UUID.randomUUID(), "민지", null)),
			new PageMeta(0, 20, 1L, 1, List.of())
		);
		when(searchUsersQueryHandler.handle(any(SearchUsersQuery.class))).thenReturn(result);

		mockMvc.perform(get("/api/v1/users")
				.with(asCurrentUser())
				.param("q", "민")
				.param("page", "0")
				.param("size", "20"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].displayName").value("민지"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
	}

	@Test
	@DisplayName("GET /users/{userId} - 존재하지 않는 사용자는 404 USER_NOT_FOUND를 반환한다")
	void getUserProfileReturns404WhenMissing() throws Exception {
		when(getUserPublicProfileQueryHandler.handle(any(GetUserPublicProfileQuery.class)))
			.thenThrow(new BusinessException(ErrorCode.USER_NOT_FOUND));

		UUID target = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/users/{userId}", target).with(asCurrentUser()))
			.andExpect(status().isNotFound())
			.andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
	}

	@Test
	@DisplayName("GET /users/{userId} - PUBLIC 프로필은 bio를 포함해 반환한다")
	void getUserProfileReturnsPublicProfile() throws Exception {
		UserPublicProfile profile = new UserPublicProfile(
			UUID.randomUUID(), "민지", null, "안녕하세요", null, null, null, null,
			UserProfileVisibility.PUBLIC, java.util.List.of(), null
		);
		when(getUserPublicProfileQueryHandler.handle(any(GetUserPublicProfileQuery.class))).thenReturn(profile);

		UUID target = UUID.randomUUID();
		mockMvc.perform(get("/api/v1/users/{userId}", target).with(asCurrentUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.displayName").value("민지"))
			.andExpect(jsonPath("$.bio").value("안녕하세요"))
			.andExpect(jsonPath("$.profileVisibility").value("PUBLIC"));
	}

	private User sampleUser() {
		UserProfile profile = new UserProfile("민지", null, null, "updated bio", UserProfileVisibility.PUBLIC);
		UserSettings settings = new UserSettings("ko", "Asia/Seoul", false, null, null, true);
		return new User(
			AUTHENTICATED_USER_ID,
			"viewer@example.com",
			null,
			UserStatus.ACTIVE,
			null,
			null,
			null,
			profile,
			settings,
			OffsetDateTime.now()
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
				.formLogin(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.exceptionHandling(exception -> exception
					.authenticationEntryPoint(authenticationEntryPoint)
					.accessDeniedHandler(accessDeniedHandler)
				)
				.authorizeHttpRequests(auth -> auth
					.requestMatchers("/actuator/health/**").permitAll()
					.anyRequest().authenticated()
				)
				.build();
		}
	}
}

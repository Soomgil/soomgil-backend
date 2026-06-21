package com.soomgil.social.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.social.api.dto.Follow;
import com.soomgil.social.api.dto.FollowStatus;
import com.soomgil.social.api.dto.PagedFollowRequest;
import com.soomgil.social.application.SocialFollowService;
import com.soomgil.user.api.dto.PagedUserSummary;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class SocialControllerTest {

	private static final UUID CURRENT = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID TARGET = UUID.fromString("10000000-0000-0000-0000-000000000002");
	private SocialFollowService service;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		service = mock(SocialFollowService.class);
		mockMvc = MockMvcBuilders.standaloneSetup(new SocialController(service)).build();
	}

	@Test
	void followsAndManagesRequestsUsingContractMethods() throws Exception {
		Follow follow = new Follow(CURRENT, TARGET, FollowStatus.ACTIVE, OffsetDateTime.parse("2026-06-20T12:00:00Z"));
		when(service.follow(CURRENT, TARGET)).thenReturn(follow);
		when(service.accept(CURRENT, TARGET)).thenReturn(new Follow(
			TARGET, CURRENT, FollowStatus.ACTIVE, follow.createdAt()
		));
		when(service.listPending(any(), anyInt(), anyInt())).thenReturn(new PagedFollowRequest(
			List.of(), new PageMeta(0, 20, 0L, 0, List.of())
		));

		mockMvc.perform(put("/api/v1/users/{userId}/follow", TARGET).principal(principal()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("ACTIVE"));
		mockMvc.perform(get("/api/v1/me/follow-requests").principal(principal()))
			.andExpect(status().isOk());
		mockMvc.perform(put("/api/v1/me/follow-requests/{userId}/accept", TARGET).principal(principal()))
			.andExpect(status().isOk());
		mockMvc.perform(delete("/api/v1/me/follow-requests/{userId}", TARGET).principal(principal()))
			.andExpect(status().isNoContent());
		mockMvc.perform(delete("/api/v1/users/{userId}/follow", TARGET).principal(principal()))
			.andExpect(status().isNoContent());

		verify(service).reject(CURRENT, TARGET);
		verify(service).unfollow(CURRENT, TARGET);
	}

	@Test
	void listsFollowersAndFollowingWithPagination() throws Exception {
		PagedUserSummary page = new PagedUserSummary(
			List.of(new UserSummary(TARGET, "Traveler", URI.create("https://example.com/profile.jpg"))),
			new PageMeta(0, 20, 1L, 1, List.of())
		);
		when(service.listFollowers(CURRENT, TARGET, 0, 20)).thenReturn(page);
		when(service.listFollowing(CURRENT, TARGET, 0, 20)).thenReturn(page);

		mockMvc.perform(get("/api/v1/users/{userId}/followers", TARGET).principal(principal()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].displayName").value("Traveler"))
			.andExpect(jsonPath("$.page.totalElements").value(1));
		mockMvc.perform(get("/api/v1/users/{userId}/following", TARGET).principal(principal()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].id").value(TARGET.toString()));
	}

	private Principal principal() {
		return () -> CURRENT.toString();
	}
}

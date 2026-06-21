package com.soomgil.planning.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.global.error.GlobalExceptionHandler;
import com.soomgil.global.error.ProblemDetailsFactory;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.global.security.ProblemDetailsAccessDeniedHandler;
import com.soomgil.global.security.ProblemDetailsAuthenticationEntryPoint;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.ChecklistItemOrder;
import com.soomgil.planning.api.dto.ChecklistMemberStatus;
import com.soomgil.planning.api.dto.CreateChecklistItemRequest;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.api.dto.ReorderChecklistItemsRequest;
import com.soomgil.planning.api.dto.UpdateChecklistItemRequest;
import com.soomgil.planning.api.dto.UpdateChecklistMemberStatusRequest;
import com.soomgil.planning.api.dto.UpsertChecklistRequest;
import com.soomgil.planning.api.dto.UpsertNoteRequest;
import com.soomgil.planning.application.handler.CreateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.DeleteChecklistCommandHandler;
import com.soomgil.planning.application.handler.DeleteChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.DeleteNoteCommandHandler;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.ListChecklistsQueryHandler;
import com.soomgil.planning.application.handler.ReorderChecklistItemsCommandHandler;
import com.soomgil.planning.application.handler.UpdateChecklistItemCommandHandler;
import com.soomgil.planning.application.handler.UpdateChecklistMemberStatusCommandHandler;
import com.soomgil.planning.application.handler.UpsertChecklistCommandHandler;
import com.soomgil.planning.application.handler.UpsertNoteCommandHandler;
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

@WebMvcTest(controllers = PlanningController.class)
@Import({
	PlanningControllerWebMvcTest.TestSecurityConfig.class,
	GlobalExceptionHandler.class,
	ProblemDetailsFactory.class,
	ProblemDetailsAuthenticationEntryPoint.class,
	ProblemDetailsAccessDeniedHandler.class
})
@TestPropertySource(properties = "soomgil.security.jwt.secret=Y2hhbmdlLW1lLXRlc3Qtc2VjcmV0LWtleS1mb3Itand0LWhzMjU2LWF0LWxlYXN0LTMyLWJ5dGVz")
class PlanningControllerWebMvcTest {

	private static final UUID USER_ID = UUID.randomUUID();
	private static final CurrentUser AUTH_USER = new CurrentUser(USER_ID, "user@example.com");

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private UpsertNoteCommandHandler upsertNoteCommandHandler;
	@MockBean
	private DeleteNoteCommandHandler deleteNoteCommandHandler;
	@MockBean
	private UpsertChecklistCommandHandler upsertChecklistCommandHandler;
	@MockBean
	private DeleteChecklistCommandHandler deleteChecklistCommandHandler;
	@MockBean
	private CreateChecklistItemCommandHandler createChecklistItemCommandHandler;
	@MockBean
	private UpdateChecklistItemCommandHandler updateChecklistItemCommandHandler;
	@MockBean
	private DeleteChecklistItemCommandHandler deleteChecklistItemCommandHandler;
	@MockBean
	private ReorderChecklistItemsCommandHandler reorderChecklistItemsCommandHandler;
	@MockBean
	private UpdateChecklistMemberStatusCommandHandler updateChecklistMemberStatusCommandHandler;
	@MockBean
	private GetNoteQueryHandler getNoteQueryHandler;
	@MockBean
	private ListChecklistsQueryHandler listChecklistsQueryHandler;

	static RequestPostProcessor asUser() {
		var authentication = new org.springframework.security.authentication
			.UsernamePasswordAuthenticationToken(AUTH_USER, null, List.of());
		return SecurityMockMvcRequestPostProcessors.authentication(authentication);
	}

	@Test
	@DisplayName("GET /planning/notes - scope 필수 query param")
	void getNoteReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();
		Note stub = new Note(noteId, tripId, PlanningScopeType.TRIP, null, "본문", null);
		when(getNoteQueryHandler.handle(any())).thenReturn(stub);

		mockMvc.perform(get("/api/v1/trips/{tripId}/planning/notes", tripId)
				.param("scopeType", "TRIP")
				.with(asUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(noteId.toString()))
			.andExpect(jsonPath("$.content").value("본문"));
	}

	@Test
	@DisplayName("GET /planning/notes - 비로그인은 401")
	void getNoteReturns401WithoutAuth() throws Exception {
		mockMvc.perform(get("/api/v1/trips/{tripId}/planning/notes", UUID.randomUUID())
				.param("scopeType", "TRIP"))
			.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("PUT /planning/notes - upsert note (200)")
	void upsertNoteReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();
		Note stub = new Note(noteId, tripId, PlanningScopeType.TRIP, null, "본문", null);
		when(upsertNoteCommandHandler.handle(any())).thenReturn(new PlanningMutationResponse(
			tripId, null, null, false, false, stub, null, null, null));

		UpsertNoteRequest body = new UpsertNoteRequest(PlanningScopeType.TRIP, null, "본문");

		mockMvc.perform(put("/api/v1/trips/{tripId}/planning/notes", tripId)
				.with(asUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.note.id").value(noteId.toString()));
	}

	@Test
	@DisplayName("DELETE /planning/notes/{noteId} - 204")
	void deleteNoteReturns204() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID noteId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/trips/{tripId}/planning/notes/{noteId}", tripId, noteId)
				.with(asUser()))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("GET /planning/checklists - 200")
	void listChecklistsReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		Checklist stub = new Checklist(checklistId, tripId, PlanningScopeType.TRIP, null,
			"제목", List.of());
		when(listChecklistsQueryHandler.handle(any())).thenReturn(List.of(stub));

		mockMvc.perform(get("/api/v1/trips/{tripId}/planning/checklists", tripId)
				.with(asUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$[0].id").value(checklistId.toString()));
	}

	@Test
	@DisplayName("PUT /planning/checklists - upsert checklist (200)")
	void upsertChecklistReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		Checklist stub = new Checklist(checklistId, tripId, PlanningScopeType.TRIP, null,
			"제목", List.of());
		when(upsertChecklistCommandHandler.handle(any())).thenReturn(new PlanningMutationResponse(
			tripId, null, null, false, false, null, stub, null, null));

		UpsertChecklistRequest body = new UpsertChecklistRequest(PlanningScopeType.TRIP, null, "제목");

		mockMvc.perform(put("/api/v1/trips/{tripId}/planning/checklists", tripId)
				.with(asUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.checklist.id").value(checklistId.toString()));
	}

	@Test
	@DisplayName("DELETE /planning/checklists/{checklistId} - 200 with cascade response")
	void deleteChecklistReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		Checklist stub = new Checklist(checklistId, tripId, PlanningScopeType.TRIP, null,
			"제목", List.of());
		when(deleteChecklistCommandHandler.handle(any())).thenReturn(new PlanningMutationResponse(
			tripId, null, null, false, false, null, stub, null, null));

		mockMvc.perform(delete("/api/v1/trips/{tripId}/planning/checklists/{checklistId}", tripId, checklistId)
				.with(asUser()))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.checklist.id").value(checklistId.toString()));
	}

	@Test
	@DisplayName("POST /planning/checklists/{checklistId}/items - 201")
	void createChecklistItemReturns201() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		ChecklistItem stub = new ChecklistItem(itemId, checklistId, 0, "본문",
			List.of(), null);
		when(createChecklistItemCommandHandler.handle(any())).thenReturn(new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, stub, null));

		CreateChecklistItemRequest body = new CreateChecklistItemRequest("본문", null);

		mockMvc.perform(post("/api/v1/trips/{tripId}/planning/checklists/{checklistId}/items", tripId, checklistId)
				.with(asUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.item.id").value(itemId.toString()));
	}

	@Test
	@DisplayName("PATCH /planning/checklists/{checklistId}/items/{itemId} - 200")
	void updateChecklistItemReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		ChecklistItem stub = new ChecklistItem(itemId, checklistId, 5, "new",
			List.of(), null);
		when(updateChecklistItemCommandHandler.handle(any())).thenReturn(new PlanningMutationResponse(
			tripId, null, null, false, false, null, null, stub, null));

		UpdateChecklistItemRequest body = new UpdateChecklistItemRequest("new", 5);

		mockMvc.perform(patch("/api/v1/trips/{tripId}/planning/checklists/{checklistId}/items/{itemId}",
				tripId, checklistId, itemId)
				.with(asUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.item.sortOrder").value(5));
	}

	@Test
	@DisplayName("DELETE /planning/checklists/{checklistId}/items/{itemId} - 204")
	void deleteChecklistItemReturns204() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();

		mockMvc.perform(delete("/api/v1/trips/{tripId}/planning/checklists/{checklistId}/items/{itemId}",
				tripId, checklistId, itemId)
				.with(asUser()))
			.andExpect(status().isNoContent());
	}

	@Test
	@DisplayName("PATCH /planning/checklists/{checklistId}/items/{itemId}/members/me - 200")
	void updateMyStatusReturns200() throws Exception {
		UUID tripId = UUID.randomUUID();
		UUID checklistId = UUID.randomUUID();
		UUID itemId = UUID.randomUUID();
		ChecklistMemberStatus stub = new ChecklistMemberStatus(null, true, null, null);
		when(updateChecklistMemberStatusCommandHandler.handle(any()))
			.thenReturn(new PlanningMutationResponse(
				tripId, null, null, false, false, null, null, null, stub));

		UpdateChecklistMemberStatusRequest body = new UpdateChecklistMemberStatusRequest(true);

		mockMvc.perform(patch("/api/v1/trips/{tripId}/planning/checklists/{checklistId}/items/{itemId}/members/me",
				tripId, checklistId, itemId)
				.with(asUser())
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.memberStatus.isCompleted").value(true));
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

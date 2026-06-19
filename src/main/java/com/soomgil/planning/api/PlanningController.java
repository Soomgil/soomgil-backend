package com.soomgil.planning.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.CreateChecklistItemRequest;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.api.dto.ReorderChecklistItemsRequest;
import com.soomgil.planning.api.dto.UpdateChecklistItemRequest;
import com.soomgil.planning.api.dto.UpdateChecklistMemberStatusRequest;
import com.soomgil.planning.api.dto.UpsertChecklistRequest;
import com.soomgil.planning.api.dto.UpsertNoteRequest;
import com.soomgil.planning.api.dto.VersionedCommandRequest;
import com.soomgil.planning.application.command.CreateChecklistItemCommand;
import com.soomgil.planning.application.command.DeleteChecklistCommand;
import com.soomgil.planning.application.command.DeleteChecklistItemCommand;
import com.soomgil.planning.application.command.DeleteNoteCommand;
import com.soomgil.planning.application.command.ReorderChecklistItemsCommand;
import com.soomgil.planning.application.command.UpdateChecklistItemCommand;
import com.soomgil.planning.application.command.UpdateChecklistMemberStatusCommand;
import com.soomgil.planning.application.command.UpsertChecklistCommand;
import com.soomgil.planning.application.command.UpsertNoteCommand;
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
import com.soomgil.planning.application.query.GetNoteQuery;
import com.soomgil.planning.application.query.ListChecklistsQuery;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 여행방 planning REST 엔드포인트.
 *
 * <p>note / checklist / checklist item / member status에 대한 CRUD를 노출한다.
 * 모든 mutation은 {@code baseVersion} 낙관적 동시성 제어를 따른다.
 * 권한은 handler 단에서 {@code TripMemberAccessChecker}로 검증한다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/planning")
@SecurityRequirement(name = "bearerAuth")
public class PlanningController extends ApiControllerSupport {

	private final UpsertNoteCommandHandler upsertNoteCommandHandler;
	private final DeleteNoteCommandHandler deleteNoteCommandHandler;
	private final UpsertChecklistCommandHandler upsertChecklistCommandHandler;
	private final DeleteChecklistCommandHandler deleteChecklistCommandHandler;
	private final CreateChecklistItemCommandHandler createChecklistItemCommandHandler;
	private final UpdateChecklistItemCommandHandler updateChecklistItemCommandHandler;
	private final DeleteChecklistItemCommandHandler deleteChecklistItemCommandHandler;
	private final ReorderChecklistItemsCommandHandler reorderChecklistItemsCommandHandler;
	private final UpdateChecklistMemberStatusCommandHandler updateChecklistMemberStatusCommandHandler;
	private final GetNoteQueryHandler getNoteQueryHandler;
	private final ListChecklistsQueryHandler listChecklistsQueryHandler;

	public PlanningController(
		UpsertNoteCommandHandler upsertNoteCommandHandler,
		DeleteNoteCommandHandler deleteNoteCommandHandler,
		UpsertChecklistCommandHandler upsertChecklistCommandHandler,
		DeleteChecklistCommandHandler deleteChecklistCommandHandler,
		CreateChecklistItemCommandHandler createChecklistItemCommandHandler,
		UpdateChecklistItemCommandHandler updateChecklistItemCommandHandler,
		DeleteChecklistItemCommandHandler deleteChecklistItemCommandHandler,
		ReorderChecklistItemsCommandHandler reorderChecklistItemsCommandHandler,
		UpdateChecklistMemberStatusCommandHandler updateChecklistMemberStatusCommandHandler,
		GetNoteQueryHandler getNoteQueryHandler,
		ListChecklistsQueryHandler listChecklistsQueryHandler
	) {
		this.upsertNoteCommandHandler = upsertNoteCommandHandler;
		this.deleteNoteCommandHandler = deleteNoteCommandHandler;
		this.upsertChecklistCommandHandler = upsertChecklistCommandHandler;
		this.deleteChecklistCommandHandler = deleteChecklistCommandHandler;
		this.createChecklistItemCommandHandler = createChecklistItemCommandHandler;
		this.updateChecklistItemCommandHandler = updateChecklistItemCommandHandler;
		this.deleteChecklistItemCommandHandler = deleteChecklistItemCommandHandler;
		this.reorderChecklistItemsCommandHandler = reorderChecklistItemsCommandHandler;
		this.updateChecklistMemberStatusCommandHandler = updateChecklistMemberStatusCommandHandler;
		this.getNoteQueryHandler = getNoteQueryHandler;
		this.listChecklistsQueryHandler = listChecklistsQueryHandler;
	}

	@GetMapping("/notes")
	public Note getNote(
		@PathVariable UUID tripId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam PlanningScopeType scopeType,
		@RequestParam(required = false) UUID itineraryDayId
	) {
		return getNoteQueryHandler.handle(new GetNoteQuery(
			tripId, scopeType, itineraryDayId, currentUser.userId()
		));
	}

	@PutMapping("/notes")
	public PlanningMutationResponse upsertNote(
		@PathVariable UUID tripId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpsertNoteRequest request
	) {
		return upsertNoteCommandHandler.handle(new UpsertNoteCommand(
			tripId,
			currentUser.userId(),
			request.baseVersion(),
			request.scopeType(),
			request.itineraryDayId(),
			request.content()
		));
	}

	@DeleteMapping("/notes/{noteId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteNote(
		@PathVariable UUID tripId,
		@PathVariable UUID noteId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		deleteNoteCommandHandler.handle(new DeleteNoteCommand(
			tripId, noteId, currentUser.userId(), request.baseVersion()
		));
	}

	@GetMapping("/checklists")
	public List<Checklist> listChecklists(
		@PathVariable UUID tripId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@RequestParam(required = false) PlanningScopeType scopeType,
		@RequestParam(required = false) UUID itineraryDayId
	) {
		return listChecklistsQueryHandler.handle(new ListChecklistsQuery(
			tripId, scopeType, itineraryDayId, currentUser.userId()
		));
	}

	@PutMapping("/checklists")
	public PlanningMutationResponse upsertChecklist(
		@PathVariable UUID tripId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpsertChecklistRequest request
	) {
		return upsertChecklistCommandHandler.handle(new UpsertChecklistCommand(
			tripId,
			currentUser.userId(),
			request.baseVersion(),
			request.scopeType(),
			request.itineraryDayId(),
			request.title()
		));
	}

	@DeleteMapping("/checklists/{checklistId}")
	public PlanningMutationResponse deleteChecklist(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return deleteChecklistCommandHandler.handle(new DeleteChecklistCommand(
			tripId, checklistId, currentUser.userId(), request.baseVersion()
		));
	}

	@PostMapping("/checklists/{checklistId}/items")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanningMutationResponse createChecklistItem(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CreateChecklistItemRequest request
	) {
		return createChecklistItemCommandHandler.handle(new CreateChecklistItemCommand(
			tripId, checklistId, currentUser.userId(),
			request.baseVersion(), request.content(), request.sortOrder()
		));
	}

	@PatchMapping("/checklists/{checklistId}/items/{itemId}")
	public PlanningMutationResponse updateChecklistItem(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@PathVariable UUID itemId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateChecklistItemRequest request
	) {
		return updateChecklistItemCommandHandler.handle(new UpdateChecklistItemCommand(
			tripId, checklistId, itemId, currentUser.userId(),
			request.baseVersion(), request.content(), request.sortOrder()
		));
	}

	@DeleteMapping("/checklists/{checklistId}/items/{itemId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteChecklistItem(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@PathVariable UUID itemId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		deleteChecklistItemCommandHandler.handle(new DeleteChecklistItemCommand(
			tripId, checklistId, itemId, currentUser.userId(), request.baseVersion()
		));
	}

	@PutMapping("/checklists/{checklistId}/items/order")
	public PlanningMutationResponse reorderChecklistItems(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody ReorderChecklistItemsRequest request
	) {
		return reorderChecklistItemsCommandHandler.handle(new ReorderChecklistItemsCommand(
			tripId, checklistId, currentUser.userId(),
			request.baseVersion(), request.itemOrders()
		));
	}

	@PatchMapping("/checklists/{checklistId}/items/{itemId}/members/me")
	public PlanningMutationResponse updateMyChecklistItemStatus(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@PathVariable UUID itemId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody UpdateChecklistMemberStatusRequest request
	) {
		return updateChecklistMemberStatusCommandHandler.handle(new UpdateChecklistMemberStatusCommand(
			tripId, checklistId, itemId, currentUser.userId(),
			request.baseVersion(), request.isCompleted()
		));
	}
}

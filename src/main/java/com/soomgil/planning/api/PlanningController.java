package com.soomgil.planning.api;

import com.soomgil.collaboration.api.dto.VersionedCommandRequest;
import com.soomgil.common.api.ApiControllerSupport;
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
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/planning")
public class PlanningController extends ApiControllerSupport {

	@GetMapping("/notes")
	public Note getNote(
		@PathVariable UUID tripId,
		@RequestParam PlanningScopeType scopeType,
		@RequestParam(required = false) UUID itineraryDayId
	) {
		return notImplemented();
	}

	@PutMapping("/notes")
	public PlanningMutationResponse upsertNote(
		@PathVariable UUID tripId,
		@Valid @RequestBody UpsertNoteRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/notes/{noteId}")
	public PlanningMutationResponse deleteNote(
		@PathVariable UUID tripId,
		@PathVariable UUID noteId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return notImplemented();
	}

	@GetMapping("/checklists")
	public List<Checklist> listChecklists(
		@PathVariable UUID tripId,
		@RequestParam(required = false) PlanningScopeType scopeType,
		@RequestParam(required = false) UUID itineraryDayId
	) {
		return notImplemented();
	}

	@PutMapping("/checklists")
	public PlanningMutationResponse upsertChecklist(
		@PathVariable UUID tripId,
		@Valid @RequestBody UpsertChecklistRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/checklists/{checklistId}")
	public PlanningMutationResponse deleteChecklist(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/checklists/{checklistId}/items")
	@ResponseStatus(HttpStatus.CREATED)
	public PlanningMutationResponse createChecklistItem(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@Valid @RequestBody CreateChecklistItemRequest request
	) {
		return notImplemented();
	}

	@PatchMapping("/checklists/{checklistId}/items/{itemId}")
	public PlanningMutationResponse updateChecklistItem(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@PathVariable UUID itemId,
		@Valid @RequestBody UpdateChecklistItemRequest request
	) {
		return notImplemented();
	}

	@DeleteMapping("/checklists/{checklistId}/items/{itemId}")
	public PlanningMutationResponse deleteChecklistItem(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@PathVariable UUID itemId,
		@Valid @RequestBody VersionedCommandRequest request
	) {
		return notImplemented();
	}

	@PutMapping("/checklists/{checklistId}/items/order")
	public PlanningMutationResponse reorderChecklistItems(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@Valid @RequestBody ReorderChecklistItemsRequest request
	) {
		return notImplemented();
	}

	@PatchMapping("/checklists/{checklistId}/items/{itemId}/members/me")
	public PlanningMutationResponse updateMyChecklistItemStatus(
		@PathVariable UUID tripId,
		@PathVariable UUID checklistId,
		@PathVariable UUID itemId,
		@Valid @RequestBody UpdateChecklistMemberStatusRequest request
	) {
		return notImplemented();
	}
}

package com.soomgil.collaboration.api;

import com.soomgil.collaboration.api.dto.CollaborationActionResponse;
import com.soomgil.collaboration.api.dto.UndoRedoRequest;
import com.soomgil.common.api.ApiControllerSupport;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/collaboration")
public class CollaborationController extends ApiControllerSupport {

	@PostMapping("/undo")
	public CollaborationActionResponse undo(
		@PathVariable UUID tripId,
		@Valid @RequestBody UndoRedoRequest request
	) {
		return notImplemented();
	}

	@PostMapping("/redo")
	public CollaborationActionResponse redo(
		@PathVariable UUID tripId,
		@Valid @RequestBody UndoRedoRequest request
	) {
		return notImplemented();
	}
}

package com.soomgil.ai.api;

import com.soomgil.ai.api.dto.AiChatSession;
import com.soomgil.ai.api.dto.AiMessageResponse;
import com.soomgil.ai.api.dto.CreateAiMessageRequest;
import com.soomgil.ai.api.dto.PagedAiChatMessage;
import com.soomgil.ai.application.AiChatService;
import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/ai")
public class AiController extends ApiControllerSupport {
	private final AiChatService aiChatService;

	public AiController(AiChatService aiChatService) {
		this.aiChatService = aiChatService;
	}

	@GetMapping("/session")
	public AiChatSession getSession(@PathVariable UUID tripId, @AuthenticationPrincipal CurrentUser currentUser) {
		return aiChatService.getSession(tripId, currentUser.userId());
	}

	@GetMapping("/messages")
	public PagedAiChatMessage listMessages(
		@PathVariable UUID tripId,
		@RequestParam(defaultValue = "0") int offset,
		@RequestParam(defaultValue = "50") int limit,
		@RequestParam(required = false) List<String> sort,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return aiChatService.listMessages(tripId, currentUser.userId(), offset, limit);
	}

	@PostMapping({"/messages", "/chat"})
	public ResponseEntity<AiMessageResponse> createMessage(
		@PathVariable UUID tripId,
		@Valid @RequestBody CreateAiMessageRequest request,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return ResponseEntity.status(HttpStatus.CREATED).body(aiChatService.createMessage(
			tripId, currentUser.userId(), request.content(), request.baseVersion(), request.viewport()
		));
	}
}

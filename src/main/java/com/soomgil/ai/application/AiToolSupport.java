package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

abstract class AiToolSupport implements AiExecutableTools {
	protected final UUID tripId;
	protected final UUID userId;
	protected final AiGuideRequest request;
	private final AiToolAuditService auditService;
	private final List<AiToolCall> executedCalls = new ArrayList<>();

	AiToolSupport(AiGuideRequest request, AiToolAuditService auditService) {
		this.tripId = request.tripId();
		this.userId = request.requesterUserId();
		this.request = request;
		this.auditService = auditService;
	}

	@Override
	public List<AiToolCall> executedCalls() {
		return List.copyOf(executedCalls);
	}

	protected Object execute(
		String toolName,
		AiToolExecutionPolicy policy,
		Object arguments,
		Long versionBefore,
		Supplier<Object> action
	) {
		UUID callId = auditService.start(request, toolName, policy, arguments, versionBefore);
		try {
			Object result = action.get();
			Long versionAfter = versionAfter(result);
			boolean undoAvailable = result instanceof ItineraryMutationResult
				&& auditService.hasCollaborationSession();
			AiToolCall call = auditService.succeed(
				callId, toolName, policy, result, versionBefore, versionAfter, undoAvailable
			);
			executedCalls.add(call);
			return result;
		}
		catch (RuntimeException exception) {
			auditService.fail(callId, exception);
			throw exception;
		}
	}

	protected long baseVersion(Long supplied) {
		if (supplied != null) return supplied;
		if (request.baseVersion() != null) return request.baseVersion();
		if (request.tripContext() != null && request.tripContext().trip() != null) {
			return request.tripContext().trip().itineraryVersion();
		}
		throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary baseVersion is required.");
	}

	protected URI uri(String value) {
		return value == null || value.isBlank() ? null : URI.create(value);
	}

	private Long versionAfter(Object result) {
		if (result instanceof ItineraryMutationResult itinerary) return itinerary.itineraryVersion();
		if (result instanceof AiAddRecommendedPlacesTools.BulkAddRecommendedPlacesResult bulkAdd) {
			return bulkAdd.versionAfter();
		}
		if (result instanceof PlanningMutationResponse planning) return planning.itineraryVersion();
		return null;
	}
}

package com.soomgil.itinerary.application.command.handler;

import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.application.port.MapObjectLeaseStore;
import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.application.command.dto.DeleteMapDrawingCommand;
import com.soomgil.itinerary.application.command.dto.DeleteMapDrawingsCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link DeleteMapDrawingCommand}를 처리해 map drawing을 soft delete한다.
 */
@Component
public class DeleteMapDrawingHandler implements CommandHandler<DeleteMapDrawingCommand, ItineraryMutationResult> {

	private final ItineraryCommandRepository repository;
	private final CollaborationCommandEventRepository eventRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;
	private final MapObjectLeaseStore leaseStore;

	@Autowired
	public DeleteMapDrawingHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider,
		MapObjectLeaseStore leaseStore
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.leaseStore = Objects.requireNonNull(leaseStore, "leaseStore must not be null");
	}

	public DeleteMapDrawingHandler(
		ItineraryCommandRepository repository,
		CollaborationCommandEventRepository eventRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider
	) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.eventRepository = Objects.requireNonNull(eventRepository, "eventRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
		this.leaseStore = null;
	}

	@Override
	@Transactional
	public ItineraryMutationResult handle(DeleteMapDrawingCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		if (command.drawingId() == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Drawing id is required.");
		}
		if (!repository.existsActiveMapDrawing(command.tripId(), command.drawingId())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
		}

		Instant now = timeProvider.now();
		if (leaseStore != null) {
			leaseStore.requireOwned(
				command.tripId(), command.drawingId(), command.actorUserId(), command.websocketSessionId(), now
			);
		}
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		boolean deleted = repository.softDeleteMapDrawing(
			command.tripId(),
			command.drawingId(),
			command.actorUserId(),
			now
		);
		if (!deleted) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
		}
		eventRepository.save(ItineraryCollaborationEvents.mapDrawingDeleted(
			command.tripId(),
			command.drawingId(),
			command.actorUserId(),
			command.baseVersion(),
			newVersion,
			command.websocketSessionId(),
			now
		));
		return new ItineraryMutationResult(
			command.tripId(),
			newVersion,
			null,
			null,
			null,
			null,
			List.of()
		);
	}

	/**
	 * 한 번의 지우개 제스처에 포함된 drawing을 같은 transaction과 command event로 삭제한다.
	 *
	 * <p>대상 중 하나라도 존재하지 않거나 lease를 소유하지 않으면 전체 요청을 실패시킨다.
	 */
	@Transactional
	public ItineraryMutationResult handle(DeleteMapDrawingsCommand command) {
		tripAccessGuard.requireActiveMember(command.tripId(), command.actorUserId());
		List<UUID> drawingIds = command.drawingIds() == null
			? List.of()
			: command.drawingIds().stream().filter(Objects::nonNull).distinct().toList();
		if (drawingIds.isEmpty() || drawingIds.size() > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Between 1 and 100 drawing ids are required.");
		}
		for (UUID drawingId : drawingIds) {
			if (!repository.existsActiveMapDrawing(command.tripId(), drawingId)) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
			}
		}

		Instant now = timeProvider.now();
		if (leaseStore != null) {
			for (UUID drawingId : drawingIds) {
				leaseStore.requireOwned(
					command.tripId(), drawingId, command.actorUserId(), command.websocketSessionId(), now
				);
			}
		}
		long newVersion = repository.incrementItineraryVersion(command.tripId(), command.baseVersion(), now)
			.orElseThrow(() -> new BusinessException(ErrorCode.CONFLICT, "Itinerary version does not match."));
		for (UUID drawingId : drawingIds) {
			if (!repository.softDeleteMapDrawing(command.tripId(), drawingId, command.actorUserId(), now)) {
				throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Map drawing was not found.");
			}
		}
		eventRepository.save(ItineraryCollaborationEvents.mapDrawingsDeleted(
			command.tripId(), drawingIds, command.actorUserId(), command.baseVersion(), newVersion,
			command.websocketSessionId(), now
		));
		return new ItineraryMutationResult(
			command.tripId(), newVersion, null, null, null, null, List.of()
		);
	}
}

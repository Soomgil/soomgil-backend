package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import com.soomgil.preference.domain.policy.PlaceTagEvidence;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceCalculator;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceInput;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEvidenceSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionUpdateRow;
import com.soomgil.preference.infrastructure.persistence.row.UserSwipeEventInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagEvidenceAdjustmentRow;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 장소 스와이프 최종 반응과 이벤트 로그를 저장한다.
 */
@Service
public class PreferenceUpsertSwipeReactionCommandHandler implements UpsertSwipeReactionCommandHandler {

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final PreferenceSwipeReactionMapper mapper;
	private final PlaceTagEvidenceCalculator evidenceCalculator;

	public PreferenceUpsertSwipeReactionCommandHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSwipeReactionMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
		this.evidenceCalculator = new PlaceTagEvidenceCalculator();
	}

	@Transactional
	@Override
	public SwipeReactionResponse handle(UpsertSwipeReactionCommand command) {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to upsert swipe reactions.");
		}

		UUID userId = provider.currentUserId();
		String placeProvider = command.provider().name();
		String reaction = command.reaction().name();
		UserPlaceReactionRow previous = mapper.findReaction(
			userId.toString(),
			placeProvider,
			command.externalPlaceId()
		);
		List<PlaceTagEvidenceSourceRow> currentTagRows = mapper.findLatestConfirmedTags(
			placeProvider,
			command.externalPlaceId()
		);
		String currentEnrichmentId = currentTagRows.isEmpty() ? null : currentTagRows.getFirst().enrichmentId();

		if (previous != null && previous.placeTagEnrichmentId() != null) {
			removePreviousEvidence(userId, previous);
		}

		if (previous == null) {
			mapper.insertReaction(new UserPlaceReactionInsertRow(
				Ids.newUuid().toString(),
				userId.toString(),
				placeProvider,
				command.externalPlaceId(),
				reaction,
				currentEnrichmentId,
				command.sourceModifiedAt()
			));
		}
		else {
			mapper.updateReaction(new UserPlaceReactionUpdateRow(
				previous.id(),
				reaction,
				currentEnrichmentId,
				command.sourceModifiedAt()
			));
		}

		mapper.insertEvent(new UserSwipeEventInsertRow(
			userId.toString(),
			placeProvider,
			command.externalPlaceId(),
			reaction,
			previous == null ? null : previous.reaction(),
			currentEnrichmentId,
			command.sourceModifiedAt()
		));
		addCurrentEvidence(userId, reaction, currentTagRows);

		return new SwipeReactionResponse(
			new PlaceRef(command.provider(), command.externalPlaceId()),
			command.reaction(),
			command.reaction() == SwipeReaction.SUPER_LIKE,
			OffsetDateTime.now()
		);
	}

	private void removePreviousEvidence(UUID userId, UserPlaceReactionRow previous) {
		List<PlaceTagEvidenceSourceRow> previousTagRows = mapper.findConfirmedTagsByEnrichment(
			previous.placeTagEnrichmentId()
		);
		for (PlaceTagEvidence evidence : calculateEvidence(previousTagRows)) {
			mapper.removeUserTagEvidence(new UserTagEvidenceAdjustmentRow(
				userId.toString(),
				evidence.tagId(),
				evidence.value(),
				previous.reaction()
			));
		}
	}

	private void addCurrentEvidence(
		UUID userId,
		String reaction,
		List<PlaceTagEvidenceSourceRow> currentTagRows
	) {
		for (PlaceTagEvidence evidence : calculateEvidence(currentTagRows)) {
			mapper.addUserTagEvidence(new UserTagEvidenceAdjustmentRow(
				userId.toString(),
				evidence.tagId(),
				evidence.value(),
				reaction
			));
		}
	}

	private List<PlaceTagEvidence> calculateEvidence(List<PlaceTagEvidenceSourceRow> rows) {
		return evidenceCalculator.calculate(rows.stream()
			.map(row -> new PlaceTagEvidenceInput(row.tagId(), row.confidence(), row.weight()))
			.toList());
	}
}

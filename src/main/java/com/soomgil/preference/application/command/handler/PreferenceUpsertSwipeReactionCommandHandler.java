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
import com.soomgil.preference.domain.policy.UserPreferenceWeightCalculator;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEvidenceSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionUpdateRow;
import com.soomgil.preference.infrastructure.persistence.row.UserSwipeEventInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagEvidenceAdjustmentRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreUpdateRow;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
	private final UserPreferenceWeightCalculator preferenceWeightCalculator;

	public PreferenceUpsertSwipeReactionCommandHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSwipeReactionMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
		this.evidenceCalculator = new PlaceTagEvidenceCalculator();
		this.preferenceWeightCalculator = new UserPreferenceWeightCalculator();
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

		List<PlaceTagEvidence> previousEvidence = previous == null || previous.placeTagEnrichmentId() == null
			? List.of()
			: calculateEvidence(mapper.findConfirmedTagsByEnrichment(previous.placeTagEnrichmentId()));
		List<PlaceTagEvidence> currentEvidence = calculateEvidence(currentTagRows);
		removePreviousEvidence(userId, previous, previousEvidence);

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
		synchronizeSavedPlace(userId, placeProvider, command.externalPlaceId(), command.reaction());
		addCurrentEvidence(userId, reaction, currentEvidence);
		recalculatePreferenceScores(userId, previousEvidence, currentEvidence);

		return new SwipeReactionResponse(
			new PlaceRef(command.provider(), command.externalPlaceId()),
			command.reaction(),
			command.reaction() == SwipeReaction.SUPER_LIKE,
			OffsetDateTime.now()
		);
	}

	private void synchronizeSavedPlace(
		UUID userId,
		String provider,
		String externalPlaceId,
		SwipeReaction reaction
	) {
		if (reaction == SwipeReaction.SUPER_LIKE) {
			mapper.upsertSuperLikeSavedPlace(
				Ids.newUuid().toString(),
				userId.toString(),
				provider,
				externalPlaceId
			);
			return;
		}
		mapper.removeSavedPlaceForNonSuperLike(userId.toString(), provider, externalPlaceId);
	}

	private void removePreviousEvidence(
		UUID userId,
		UserPlaceReactionRow previous,
		List<PlaceTagEvidence> previousEvidence
	) {
		if (previous == null) {
			return;
		}
		for (PlaceTagEvidence evidence : previousEvidence) {
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
		List<PlaceTagEvidence> currentEvidence
	) {
		for (PlaceTagEvidence evidence : currentEvidence) {
			mapper.addUserTagEvidence(new UserTagEvidenceAdjustmentRow(
				userId.toString(),
				evidence.tagId(),
				evidence.value(),
				reaction
			));
		}
	}

	private void recalculatePreferenceScores(
		UUID userId,
		List<PlaceTagEvidence> previousEvidence,
		List<PlaceTagEvidence> currentEvidence
	) {
		Set<String> affectedTagIds = new LinkedHashSet<>();
		previousEvidence.forEach(evidence -> affectedTagIds.add(evidence.tagId()));
		currentEvidence.forEach(evidence -> affectedTagIds.add(evidence.tagId()));

		for (String tagId : affectedTagIds) {
			UserTagPreferenceScoreSourceRow source = mapper.findUserTagPreferenceScoreSource(
				userId.toString(),
				tagId
			);
			if (source == null) {
				continue;
			}
			mapper.updateUserTagPreferenceScore(new UserTagPreferenceScoreUpdateRow(
				userId.toString(),
				tagId,
				preferenceWeightCalculator.calculatePreferenceScore(
					source.smoothedPositiveRate(),
					source.preferenceDiscrimination(),
					source.positiveEvidence(),
					source.negativeEvidence()
				),
				"preference-score-odds-v1"
			));
		}
	}

	private List<PlaceTagEvidence> calculateEvidence(List<PlaceTagEvidenceSourceRow> rows) {
		return evidenceCalculator.calculate(rows.stream()
			.map(row -> new PlaceTagEvidenceInput(row.tagId(), row.confidence(), row.weight()))
			.toList());
	}

	@Transactional
	public void refreshPlaceEnrichment(
		String provider,
		String externalPlaceId,
		OffsetDateTime sourceModifiedAt
	) {
		List<PlaceTagEvidenceSourceRow> currentTagRows = mapper.findLatestConfirmedTags(provider, externalPlaceId);
		if (currentTagRows.isEmpty()) {
			return;
		}
		String enrichmentId = currentTagRows.getFirst().enrichmentId();
		List<PlaceTagEvidence> currentEvidence = calculateEvidence(currentTagRows);
		for (var reaction : mapper.findReactionsNeedingEnrichmentRefresh(provider, externalPlaceId, enrichmentId)) {
			UUID userId = UUID.fromString(reaction.userId());
			List<PlaceTagEvidence> previousEvidence = reaction.placeTagEnrichmentId() == null
				? List.of() : calculateEvidence(mapper.findConfirmedTagsByEnrichment(reaction.placeTagEnrichmentId()));
			UserPlaceReactionRow previous = new UserPlaceReactionRow(
				reaction.id(), reaction.reaction(), reaction.placeTagEnrichmentId()
			);
			removePreviousEvidence(userId, previous, previousEvidence);
			mapper.updateReactionEnrichment(reaction.id(), enrichmentId, sourceModifiedAt);
			addCurrentEvidence(userId, reaction.reaction(), currentEvidence);
			recalculatePreferenceScores(userId, previousEvidence, currentEvidence);
		}
	}
}

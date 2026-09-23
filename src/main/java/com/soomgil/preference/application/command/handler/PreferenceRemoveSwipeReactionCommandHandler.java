package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.NoResult;
import com.soomgil.global.cache.InvalidatesMyPageCache;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.preference.application.command.dto.RemoveSwipeReactionCommand;
import com.soomgil.preference.domain.policy.PlaceTagEvidence;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceCalculator;
import com.soomgil.preference.domain.policy.PlaceTagEvidenceInput;
import com.soomgil.preference.domain.policy.UserPreferenceWeightCalculator;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagEvidenceAdjustmentRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreUpdateRow;
import com.soomgil.preference.infrastructure.websocket.PreferenceReactionRealtimePublisher;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 장소 반응과 그 반응에서 파생된 취향 근거를 함께 제거한다.
 *
 * <p>SUPER_LIKE 취소 시 연결된 저장 장소도 같은 transaction에서 해제한다.
 * 이미 반응이 없는 요청은 성공으로 처리한다.
 */
@Service
public class PreferenceRemoveSwipeReactionCommandHandler implements RemoveSwipeReactionCommandHandler {

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final PreferenceSwipeReactionMapper mapper;
	private final PreferenceReactionRealtimePublisher realtimePublisher;
	private final PlaceTagEvidenceCalculator evidenceCalculator = new PlaceTagEvidenceCalculator();
	private final UserPreferenceWeightCalculator preferenceWeightCalculator = new UserPreferenceWeightCalculator();

	public PreferenceRemoveSwipeReactionCommandHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSwipeReactionMapper mapper,
		PreferenceReactionRealtimePublisher realtimePublisher
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
		this.realtimePublisher = realtimePublisher;
	}

	@Override
	@Transactional
	@InvalidatesMyPageCache({"preferences", "saved"})
	public NoResult handle(RemoveSwipeReactionCommand command) {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to remove swipe reactions.");
		}

		UUID userId = provider.currentUserId();
		String placeProvider = command.provider().name();
		UserPlaceReactionRow previous = mapper.findReaction(
			userId.toString(),
			placeProvider,
			command.externalPlaceId()
		);
		if (previous == null) {
			return NoResult.INSTANCE;
		}

		List<PlaceTagEvidence> previousEvidence = previous.placeTagEnrichmentId() == null
			? List.of()
			: evidenceCalculator.calculate(mapper.findConfirmedTagsByEnrichment(previous.placeTagEnrichmentId())
				.stream()
				.map(row -> new PlaceTagEvidenceInput(row.tagId(), row.confidence(), row.weight()))
				.toList());

		for (PlaceTagEvidence evidence : previousEvidence) {
			mapper.removeUserTagEvidence(new UserTagEvidenceAdjustmentRow(
				userId.toString(),
				evidence.tagId(),
				evidence.value(),
				previous.reaction()
			));
		}

		mapper.deleteReaction(previous.id());
		if ("SUPER_LIKE".equals(previous.reaction())) {
			mapper.removeSavedPlaceForNonSuperLike(
				userId.toString(),
				placeProvider,
				command.externalPlaceId()
			);
		}

		for (PlaceTagEvidence evidence : previousEvidence) {
			UserTagPreferenceScoreSourceRow source = mapper.findUserTagPreferenceScoreSource(
				userId.toString(),
				evidence.tagId()
			);
			if (source == null) {
				continue;
			}
			mapper.updateUserTagPreferenceScore(new UserTagPreferenceScoreUpdateRow(
				userId.toString(),
				evidence.tagId(),
				preferenceWeightCalculator.calculatePreferenceScore(
					source.smoothedPositiveRate(),
					source.preferenceDiscrimination(),
					source.positiveEvidence(),
					source.negativeEvidence()
				),
				"preference-score-odds-v1"
			));
		}

		realtimePublisher.publish(userId);
		return NoResult.INSTANCE;
	}
}

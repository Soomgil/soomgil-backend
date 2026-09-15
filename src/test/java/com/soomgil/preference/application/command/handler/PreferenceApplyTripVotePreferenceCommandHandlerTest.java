package com.soomgil.preference.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceCommand;
import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceResult;
import com.soomgil.preference.application.command.dto.TripVoteStickerPlace;
import com.soomgil.preference.domain.policy.TripVoteEvidencePolicy;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceTripVoteMapper;
import com.soomgil.preference.infrastructure.persistence.row.PlaceTagEvidenceSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreSourceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagVoteEvidenceRow;
import com.soomgil.preference.infrastructure.persistence.row.UserTagPreferenceScoreUpdateRow;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PreferenceApplyTripVotePreferenceCommandHandlerTest {

	private static final String ENRICHMENT_ID = UUID.randomUUID().toString();
	private static final String TAG_A = UUID.randomUUID().toString();
	private static final String TAG_B = UUID.randomUUID().toString();

	private final PreferenceTripVoteMapper voteMapper = mock(PreferenceTripVoteMapper.class);
	private final PreferenceSwipeReactionMapper swipeMapper = mock(PreferenceSwipeReactionMapper.class);

	private final PreferenceApplyTripVotePreferenceCommandHandler handler =
		new PreferenceApplyTripVotePreferenceCommandHandler(
			voteMapper,
			swipeMapper,
			new TripVoteEvidencePolicy(new BigDecimal("1.0"), new BigDecimal("0.5"), new BigDecimal("2.0"))
		);

	private final UUID sessionId = UUID.randomUUID();
	private final UUID userId = UUID.randomUUID();

	@Test
	@DisplayName("스티커를 붙인 장소의 태그에 비율만큼 근거를 가산한다")
	void appliesEvidenceProportionallyToTags() {
		stubTwoEqualTags();
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(1);

		ApplyTripVotePreferenceResult result = handler.handle(command(place("126508", 2)));

		assertThat(result.appliedPlaceCount()).isEqualTo(1);
		ArgumentCaptor<UserTagVoteEvidenceRow> captor = ArgumentCaptor.forClass(UserTagVoteEvidenceRow.class);
		verify(voteMapper, times(2)).addUserTagVoteEvidence(captor.capture());
		// 스티커 2개 = 1.5 units, 태그 2개가 0.5씩 나눠 가지므로 각각 0.75
		assertThat(captor.getAllValues())
			.extracting(UserTagVoteEvidenceRow::evidence)
			.allSatisfy(value -> assertThat(value).isEqualByComparingTo("0.75"));
	}

	@Test
	@DisplayName("선정 여부와 무관하게 스티커를 붙인 모든 장소를 반영한다")
	void appliesEveryStickeredPlace() {
		stubTwoEqualTags();
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(1);

		ApplyTripVotePreferenceResult result = handler.handle(command(
			place("126508", 3), place("126509", 1), place("126510", 1)
		));

		assertThat(result.appliedPlaceCount()).isEqualTo(3);
		verify(voteMapper, times(3)).insertEvidenceIfAbsent(any());
	}

	@Test
	@DisplayName("장소별 스티커 개수를 근거 row에 그대로 보존한다")
	void preservesStickerCountPerPlace() {
		stubTwoEqualTags();
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(1);

		handler.handle(command(place("126508", 4)));

		ArgumentCaptor<com.soomgil.preference.infrastructure.persistence.row.UserPlaceVoteEvidenceInsertRow> captor =
			ArgumentCaptor.forClass(
				com.soomgil.preference.infrastructure.persistence.row.UserPlaceVoteEvidenceInsertRow.class
			);
		verify(voteMapper).insertEvidenceIfAbsent(captor.capture());
		assertThat(captor.getValue().stickerCount()).isEqualTo(4);
		assertThat(captor.getValue().source()).isEqualTo("TRIP_VOTE");
		assertThat(captor.getValue().calculationVersion()).isEqualTo("trip-vote-evidence-v1");
		assertThat(captor.getValue().voteSessionId()).isEqualTo(sessionId.toString());
	}

	@Test
	@DisplayName("같은 투표 제출을 재시도해도 취향 근거가 중복 반영되지 않는다")
	void isIdempotentForSameSessionUserAndPlace() {
		stubTwoEqualTags();
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(0);

		ApplyTripVotePreferenceResult result = handler.handle(command(place("126508", 2)));

		assertThat(result.appliedPlaceCount()).isZero();
		assertThat(result.skippedAlreadyAppliedCount()).isEqualTo(1);
		verify(voteMapper, never()).addUserTagVoteEvidence(any());
		verify(swipeMapper, never()).updateUserTagPreferenceScore(any());
	}

	@Test
	@DisplayName("확정 태그가 없는 장소는 태그 근거를 만들지 않지만 감사 row는 남긴다")
	void placeWithoutConfirmedTagsStillRecordsAudit() {
		when(swipeMapper.findLatestConfirmedTags(eq("KTO"), anyString())).thenReturn(List.of());
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(1);

		ApplyTripVotePreferenceResult result = handler.handle(command(place("126508", 2)));

		assertThat(result.appliedPlaceCount()).isEqualTo(1);
		verify(voteMapper).insertEvidenceIfAbsent(any());
		verify(voteMapper, never()).addUserTagVoteEvidence(any());
	}

	@Test
	@DisplayName("스와이프 최종 반응과 저장 장소는 건드리지 않는다")
	void doesNotTouchSwipeReactionsOrSavedPlaces() {
		stubTwoEqualTags();
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(1);

		handler.handle(command(place("126508", 2)));

		verify(swipeMapper, never()).insertReaction(any());
		verify(swipeMapper, never()).updateReaction(any());
		verify(swipeMapper, never()).insertEvent(any());
		verify(swipeMapper, never()).upsertSuperLikeSavedPlace(anyString(), anyString(), anyString(), anyString());
		verify(swipeMapper, never()).addUserTagEvidence(any());
		verify(swipeMapper, never()).removeUserTagEvidence(any());
	}

	@Test
	@DisplayName("근거를 가산한 태그는 기존 계산기로 preference_score를 다시 계산한다")
	void recalculatesPreferenceScoreWithExistingCalculator() {
		stubTwoEqualTags();
		when(voteMapper.insertEvidenceIfAbsent(any())).thenReturn(1);
		when(swipeMapper.findUserTagPreferenceScoreSource(eq(userId.toString()), anyString()))
			.thenReturn(new UserTagPreferenceScoreSourceRow(
				TAG_A, new BigDecimal("3.0"), new BigDecimal("1.0"),
				new BigDecimal("0.6"), new BigDecimal("0.4")
			));

		handler.handle(command(place("126508", 2)));

		ArgumentCaptor<UserTagPreferenceScoreUpdateRow> captor =
			ArgumentCaptor.forClass(UserTagPreferenceScoreUpdateRow.class);
		verify(swipeMapper, times(2)).updateUserTagPreferenceScore(captor.capture());
		assertThat(captor.getAllValues().get(0).userId()).isEqualTo(userId.toString());
	}

	@Test
	@DisplayName("스티커가 0개인 장소는 요청에 들어와도 반영하지 않는다")
	void ignoresPlacesWithoutStickers() {
		stubTwoEqualTags();

		ApplyTripVotePreferenceResult result = handler.handle(command(place("126508", 0)));

		assertThat(result.appliedPlaceCount()).isZero();
		verify(voteMapper, never()).insertEvidenceIfAbsent(any());
	}

	@Test
	@DisplayName("빈 목록이면 아무것도 하지 않는다")
	void emptyRequestIsNoOp() {
		ApplyTripVotePreferenceResult result = handler.handle(
			new ApplyTripVotePreferenceCommand(sessionId, userId, List.of())
		);

		assertThat(result.appliedPlaceCount()).isZero();
		assertThat(result.skippedAlreadyAppliedCount()).isZero();
		verify(voteMapper, never()).insertEvidenceIfAbsent(any());
	}

	private void stubTwoEqualTags() {
		when(swipeMapper.findLatestConfirmedTags(eq("KTO"), anyString())).thenReturn(List.of(
			new PlaceTagEvidenceSourceRow(ENRICHMENT_ID, TAG_A, new BigDecimal("0.8"), new BigDecimal("1.0")),
			new PlaceTagEvidenceSourceRow(ENRICHMENT_ID, TAG_B, new BigDecimal("0.8"), new BigDecimal("1.0"))
		));
	}

	private ApplyTripVotePreferenceCommand command(TripVoteStickerPlace... places) {
		return new ApplyTripVotePreferenceCommand(sessionId, userId, List.of(places));
	}

	private TripVoteStickerPlace place(String externalPlaceId, int stickerCount) {
		return new TripVoteStickerPlace("KTO", externalPlaceId, stickerCount);
	}
}

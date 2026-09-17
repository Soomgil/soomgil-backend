package com.soomgil.voting.application.command.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.geo.application.query.dto.FindLegalRegionsByCodesQuery;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.application.query.handler.FindLegalRegionsByCodesHandler;
import com.soomgil.geo.domain.model.LegalRegionLevel;
import com.soomgil.trip.application.query.dto.ListTripRegionCodesQuery;
import com.soomgil.trip.application.query.handler.ListTripRegionCodesHandler;
import com.soomgil.voting.api.dto.TripVoteRegion;
import com.soomgil.voting.application.port.VoteRegionRecord;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.application.query.dto.ListTripVoteCandidatesQuery;
import com.soomgil.preference.application.query.dto.TripVoteCandidateView;
import com.soomgil.preference.application.query.handler.ListTripVoteCandidatesQueryHandler;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.voting.api.dto.TripVoteSessionDetail;
import com.soomgil.voting.application.command.dto.OpenVoteSessionCommand;
import com.soomgil.voting.application.port.VoteCandidateRecord;
import com.soomgil.voting.application.port.VoteParticipantRecord;
import com.soomgil.voting.application.port.VoteSessionRecord;
import com.soomgil.voting.application.port.VoteSessionRepository;
import com.soomgil.voting.application.service.VoteSessionAssembler;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import java.time.Instant;
import com.soomgil.voting.application.port.VoteNotificationPublisher;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class OpenVoteSessionHandlerTest {

	private static final Instant NOW = Instant.parse("2026-08-24T10:00:00Z");

	private final VoteNotificationPublisher notifications = mock(VoteNotificationPublisher.class);
	private final VoteSessionRepository repository = mock(VoteSessionRepository.class);
	private final TripAccessGuard tripAccessGuard = mock(TripAccessGuard.class);
	private final ListTripMembersHandler membersHandler = mock(ListTripMembersHandler.class);
	private final ListTripVoteCandidatesQueryHandler candidatesHandler =
		mock(ListTripVoteCandidatesQueryHandler.class);
	private final FindTripDetailHandler tripDetailHandler = mock(FindTripDetailHandler.class);
	private final ListTripRegionCodesHandler regionCodesHandler = mock(ListTripRegionCodesHandler.class);
	private final FindLegalRegionsByCodesHandler legalRegionsHandler = mock(FindLegalRegionsByCodesHandler.class);

	private final OpenVoteSessionHandler handler = new OpenVoteSessionHandler(
		repository, tripAccessGuard, membersHandler, tripDetailHandler, candidatesHandler,
		regionCodesHandler, legalRegionsHandler, new VoteSessionAssembler(), () -> NOW, notifications
	);

	private final UUID tripId = UUID.randomUUID();
	private final UUID ownerId = UUID.randomUUID();
	private final UUID memberId = UUID.randomUUID();

	@BeforeEach
	void stubDefaults() {
		when(repository.findActiveByTripId(tripId)).thenReturn(Optional.empty());
		when(membersHandler.handle(any(ListTripMembersQuery.class)))
			.thenReturn(List.of(member(ownerId), member(memberId)));
		when(candidatesHandler.handle(any(ListTripVoteCandidatesQuery.class))).thenReturn(candidates(10));
		when(tripDetailHandler.handle(any(FindTripDetailQuery.class))).thenReturn(tripDetail("제주"));
		when(regionCodesHandler.handle(any(ListTripRegionCodesQuery.class))).thenReturn(List.of("5011000000"));
		when(legalRegionsHandler.handle(any(FindLegalRegionsByCodesQuery.class))).thenReturn(List.of(
			new LegalRegionView("5011000000", "제주시", "제주특별자치도 제주시", LegalRegionLevel.SIGUNGU, "5000000000", true)
		));
	}

	@Test
	@DisplayName("방장이 투표를 시작하면 활성 참여자 전원이 참여자로 확정된다")
	void confirmsAllActiveMembersAsParticipants() {
		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<VoteParticipantRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).insertParticipants(captor.capture());
		verify(notifications).publish(eq(tripId), any(UUID.class), eq(false), eq(NOW));
		assertThat(captor.getValue()).hasSize(2);
		assertThat(captor.getValue())
			.extracting(VoteParticipantRecord::userId)
			.containsExactlyInAnyOrder(ownerId, memberId);
		assertThat(captor.getValue())
			.allSatisfy(participant -> {
				assertThat(participant.status()).isEqualTo(VoteParticipantStatus.NOT_STARTED);
				assertThat(participant.stickerAllowance()).isEqualTo(5);
			});
	}

	@Test
	@DisplayName("후보는 투표 시작 시점 snapshot으로 고정 저장된다")
	void snapshotsCandidatesAtOpenTime() {
		TripVoteSessionDetail detail = handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<VoteCandidateRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).insertCandidates(captor.capture());
		assertThat(captor.getValue()).hasSize(10);
		assertThat(captor.getValue().get(0).sortOrder()).isEqualTo(1);
		assertThat(detail.candidateCount()).isEqualTo(10);
		assertThat(detail.status()).isEqualTo(VoteSessionStatus.OPEN);
	}

	@Test
	@DisplayName("기본 후보 수는 10개다")
	void defaultsToTenCandidates() {
		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		ArgumentCaptor<ListTripVoteCandidatesQuery> captor =
			ArgumentCaptor.forClass(ListTripVoteCandidatesQuery.class);
		verify(candidatesHandler).handle(captor.capture());
		assertThat(captor.getValue().limit()).isEqualTo(10);
	}

	@Test
	@DisplayName("진행 중인 세션이 있으면 새로 시작할 수 없다")
	void rejectsWhenActiveSessionExists() {
		when(repository.findActiveByTripId(tripId)).thenReturn(Optional.of(existingSession()));

		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_SESSION_ALREADY_OPEN));

		verify(repository, never()).insertSession(any(VoteSessionRecord.class));
	}

	@Test
	@DisplayName("방장이 아니면 투표를 시작할 수 없다")
	void nonOwnerCannotOpenSession() {
		when(tripAccessGuard.requireOwner(tripId, memberId))
			.thenThrow(new BusinessException(ErrorCode.FORBIDDEN, "Trip owner access is required."));

		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, memberId, 5, 3, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode()).isEqualTo(ErrorCode.FORBIDDEN));

		verify(repository, never()).insertSession(any(VoteSessionRecord.class));
	}

	@Test
	@DisplayName("스티커 지급 개수가 후보 수를 넘으면 거절한다")
	void rejectsStickerAllowanceAboveCandidateCount() {
		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 11, 3, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	@Test
	@DisplayName("선정 개수가 후보 수를 넘으면 후보 부족으로 거절한다")
	void rejectsSelectionCountAboveCandidateCount() {
		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 11, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_CANDIDATE_POOL_INSUFFICIENT));
	}

	@Test
	@DisplayName("추천이 선정 개수보다 적은 후보를 만들면 시작하지 않는다")
	void rejectsWhenCandidatePoolIsTooSmall() {
		when(candidatesHandler.handle(any(ListTripVoteCandidatesQuery.class))).thenReturn(candidates(2));

		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 2, 3, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_CANDIDATE_POOL_INSUFFICIENT));

		verify(repository, never()).insertSession(any(VoteSessionRecord.class));
	}

	@Test
	@DisplayName("후보를 하나도 만들지 못하면 시작하지 않는다")
	void rejectsWhenNoCandidateWasBuilt() {
		when(candidatesHandler.handle(any(ListTripVoteCandidatesQuery.class))).thenReturn(List.of());

		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 1, 1, null)))
			.isInstanceOf(BusinessException.class)
			.satisfies(ex -> assertThat(((BusinessException) ex).errorCode())
				.isEqualTo(ErrorCode.VOTE_CANDIDATE_POOL_INSUFFICIENT));
	}

	@Test
	@DisplayName("세션은 DRAFT가 아니라 OPEN 상태로 생성된다")
	void createsSessionInOpenStatus() {
		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		ArgumentCaptor<VoteSessionRecord> captor = ArgumentCaptor.forClass(VoteSessionRecord.class);
		verify(repository).insertSession(captor.capture());
		assertThat(captor.getValue().status()).isEqualTo(VoteSessionStatus.OPEN);
		assertThat(captor.getValue().status()).isNotEqualTo(VoteSessionStatus.DRAFT);
		assertThat(captor.getValue().stickerAllowance()).isEqualTo(5);
		assertThat(captor.getValue().selectionCount()).isEqualTo(3);
	}

	@Test
	@DisplayName("여행방 대표 목적지를 후보 생성 query에 대체 검색어로 전달한다")
	void passesDisplayDestinationAsCandidateKeyword() {
		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		ArgumentCaptor<ListTripVoteCandidatesQuery> captor =
			ArgumentCaptor.forClass(ListTripVoteCandidatesQuery.class);
		verify(candidatesHandler).handle(captor.capture());
		assertThat(captor.getValue().destinationKeyword()).isEqualTo("제주");
	}

	@Test
	@DisplayName("대표 목적지가 없으면 대체 검색어 없이 후보를 만든다")
	void passesNullKeywordWhenDestinationIsMissing() {
		when(tripDetailHandler.handle(any(FindTripDetailQuery.class))).thenReturn(tripDetail(null));

		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		ArgumentCaptor<ListTripVoteCandidatesQuery> captor =
			ArgumentCaptor.forClass(ListTripVoteCandidatesQuery.class);
		verify(candidatesHandler).handle(captor.capture());
		assertThat(captor.getValue().destinationKeyword()).isNull();
	}

	private TripDetailView tripDetail(String displayDestination) {
		return new TripDetailView(
			tripId, "제주 여행", displayDestination, TripStatus.ACTIVE, TripAccessRole.OWNER,
			0L, NOW, ownerId, List.of(), null
		);
	}

	private TripMemberView member(UUID userId) {
		return new TripMemberView(
			UUID.randomUUID(), tripId, userId, TripMemberRole.MEMBER,
			userId.equals(ownerId) ? TripAccessRole.OWNER : TripAccessRole.MEMBER,
			TripMemberStatus.ACTIVE, NOW
		);
	}

	private List<TripVoteCandidateView> candidates(int count) {
		List<TripVoteCandidateView> views = new ArrayList<>();
		for (int index = 1; index <= count; index++) {
			views.add(new TripVoteCandidateView(
				index, PlaceProvider.KTO, "place-" + index, "장소 " + index,
				"제주특별자치도", 33.45, 126.94, null, "A01"
			));
		}
		return views;
	}

	private VoteSessionRecord existingSession() {
		return new VoteSessionRecord(
			UUID.randomUUID(), tripId, VoteSessionStatus.OPEN, ownerId, 5, 3, 10, NOW, null, null, null, null
		);
	}
	@Test
	@DisplayName("방장이 고른 지역이 있으면 여행방 지역 대신 그 지역으로 후보를 만든다")
	void usesRequestedRegionsInsteadOfTripRegions() {
		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null, List.of("5013000000")));

		ArgumentCaptor<ListTripVoteCandidatesQuery> captor = ArgumentCaptor.forClass(ListTripVoteCandidatesQuery.class);
		verify(candidatesHandler).handle(captor.capture());
		assertThat(captor.getValue().regionCodes()).containsExactly("5013000000");
		verify(regionCodesHandler, never()).handle(any(ListTripRegionCodesQuery.class));
	}

	@Test
	@DisplayName("지역을 고르지 않으면 여행방에 등록된 지역을 그대로 쓴다")
	void fallsBackToTripRegionsWhenRequestHasNone() {
		handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		ArgumentCaptor<ListTripVoteCandidatesQuery> captor = ArgumentCaptor.forClass(ListTripVoteCandidatesQuery.class);
		verify(candidatesHandler).handle(captor.capture());
		assertThat(captor.getValue().regionCodes()).containsExactly("5011000000");
	}

	@Test
	@DisplayName("지역도 목적지도 없으면 후보를 만들 수 없어 시작을 거절한다")
	void rejectsWhenNoRegionAndNoDestination() {
		when(regionCodesHandler.handle(any(ListTripRegionCodesQuery.class))).thenReturn(List.of());
		when(tripDetailHandler.handle(any(FindTripDetailQuery.class))).thenReturn(tripDetail(""));

		assertThatThrownBy(() -> handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null)))
			.isInstanceOf(BusinessException.class);
		verify(candidatesHandler, never()).handle(any(ListTripVoteCandidatesQuery.class));
	}

	@Test
	@DisplayName("투표를 시작하면 사용한 지역을 이름과 함께 세션에 고정한다")
	void snapshotsRegionsWithNames() {
		TripVoteSessionDetail detail = handler.handle(new OpenVoteSessionCommand(tripId, ownerId, 5, 3, null));

		@SuppressWarnings("unchecked")
		ArgumentCaptor<List<VoteRegionRecord>> captor = ArgumentCaptor.forClass(List.class);
		verify(repository).insertRegions(captor.capture());
		assertThat(captor.getValue())
			.extracting(VoteRegionRecord::legalRegionCode, VoteRegionRecord::regionName, VoteRegionRecord::sortOrder)
			.containsExactly(tuple("5011000000", "제주시", 0));
		assertThat(detail.regions()).containsExactly(new TripVoteRegion("5011000000", "제주시"));
	}
}

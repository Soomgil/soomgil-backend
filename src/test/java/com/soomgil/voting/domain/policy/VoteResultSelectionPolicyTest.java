package com.soomgil.voting.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 투표 결과에서 최종 선정 관광지를 고르는 정책 테스트.
 */
class VoteResultSelectionPolicyTest {

	private final VoteResultSelectionPolicy policy = new VoteResultSelectionPolicy();

	private final UUID a = UUID.randomUUID();
	private final UUID b = UUID.randomUUID();
	private final UUID c = UUID.randomUUID();
	private final UUID d = UUID.randomUUID();

	@Test
	@DisplayName("스티커 총합이 높은 후보부터 선정 개수만큼 고른다")
	void selectsTopCandidatesByStickerCount() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(a, 1, 2),
			tally(b, 2, 7),
			tally(c, 3, 5)
		), 2);

		assertThat(selected).extracting(VoteCandidateTally::candidateId).containsExactly(b, c);
		assertThat(selected).extracting(VoteCandidateTally::selectedRank).containsExactly(1, 2);
	}

	@Test
	@DisplayName("마지막 선정 순위가 동점이면 동점 후보를 모두 선정한다")
	void selectsAllTiedCandidatesAtCutoff() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(a, 1, 9),
			tally(b, 2, 4),
			tally(c, 3, 4),
			tally(d, 4, 1)
		), 2);

		assertThat(selected).hasSize(3);
		assertThat(selected).extracting(VoteCandidateTally::candidateId).containsExactly(a, b, c);
		assertThat(selected).extracting(VoteCandidateTally::selectedRank).containsExactly(1, 2, 2);
	}

	@Test
	@DisplayName("1위가 여러 개로 동점이고 선정 개수가 1이면 모두 선정한다")
	void selectsAllWhenTopIsTiedAndSelectionCountIsOne() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(a, 1, 5),
			tally(b, 2, 5),
			tally(c, 3, 5),
			tally(d, 4, 2)
		), 1);

		assertThat(selected).hasSize(3);
		assertThat(selected).extracting(VoteCandidateTally::selectedRank).containsExactly(1, 1, 1);
	}

	@Test
	@DisplayName("스티커를 하나도 받지 못한 후보는 선정하지 않는다")
	void neverSelectsCandidatesWithoutStickers() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(a, 1, 3),
			tally(b, 2, 0),
			tally(c, 3, 0)
		), 3);

		assertThat(selected).hasSize(1);
		assertThat(selected.get(0).candidateId()).isEqualTo(a);
	}

	@Test
	@DisplayName("아무도 스티커를 붙이지 않았으면 선정 결과가 비어 있다")
	void selectsNothingWhenNoStickerWasUsed() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(a, 1, 0),
			tally(b, 2, 0)
		), 2);

		assertThat(selected).isEmpty();
	}

	@Test
	@DisplayName("스티커를 받은 후보가 선정 개수보다 적으면 있는 만큼만 선정한다")
	void selectsFewerThanRequestedWhenNotEnoughStickeredCandidates() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(a, 1, 2),
			tally(b, 2, 0),
			tally(c, 3, 0)
		), 3);

		assertThat(selected).hasSize(1);
	}

	@Test
	@DisplayName("스티커 수가 같으면 후보 snapshot 순서를 유지한다")
	void keepsSnapshotOrderForEqualStickerCounts() {
		List<VoteCandidateTally> selected = policy.select(List.of(
			tally(c, 3, 4),
			tally(a, 1, 4),
			tally(b, 2, 4)
		), 3);

		assertThat(selected).extracting(VoteCandidateTally::sortOrder).containsExactly(1, 2, 3);
	}

	@Test
	@DisplayName("후보가 비어 있으면 빈 결과를 반환한다")
	void handlesEmptyCandidateList() {
		assertThat(policy.select(List.of(), 3)).isEmpty();
	}

	private VoteCandidateTally tally(UUID candidateId, int sortOrder, int stickerCount) {
		return new VoteCandidateTally(candidateId, sortOrder, stickerCount, false, null);
	}
}

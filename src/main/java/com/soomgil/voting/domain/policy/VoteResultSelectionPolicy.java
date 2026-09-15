package com.soomgil.voting.domain.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 투표 결과에서 최종 선정 관광지를 고르는 정책.
 *
 * <p>규칙:
 * <ol>
 *   <li>스티커 총합이 높은 후보부터 방장이 정한 선정 개수만큼 선택한다.</li>
 *   <li>마지막 선정 순위가 동점이면 동점 후보를 모두 선정한다. 이때 결과 개수가 선정 개수를 넘을 수 있다.</li>
 *   <li>스티커를 하나도 받지 못한 후보는 선정 개수가 남아 있어도 선정하지 않는다.</li>
 *   <li>스티커 수가 같으면 투표 시작 시점 snapshot 순서를 유지해 결과가 매번 같도록 한다.</li>
 * </ol>
 */
public class VoteResultSelectionPolicy {

	/**
	 * 후보 집계에서 최종 선정 목록을 계산한다.
	 *
	 * @param tallies 후보별 스티커 집계. 순서는 상관없다
	 * @param selectionCount 방장이 정한 선정 개수
	 * @return 선정 순위가 부여된 선정 후보 목록. 스티커를 받은 후보가 없으면 빈 목록
	 */
	public List<VoteCandidateTally> select(List<VoteCandidateTally> tallies, int selectionCount) {
		if (tallies == null || tallies.isEmpty() || selectionCount < 1) {
			return List.of();
		}

		List<VoteCandidateTally> ordered = tallies.stream()
			.filter(tally -> tally.stickerCount() > 0)
			.sorted(Comparator
				.comparingInt(VoteCandidateTally::stickerCount).reversed()
				.thenComparingInt(VoteCandidateTally::sortOrder))
			.toList();
		if (ordered.isEmpty()) {
			return List.of();
		}

		// 컷오프에 걸린 스티커 수. 이 값과 같은 후보는 선정 개수를 넘더라도 모두 선정한다.
		int cutoffIndex = Math.min(selectionCount, ordered.size()) - 1;
		int cutoffStickerCount = ordered.get(cutoffIndex).stickerCount();

		List<VoteCandidateTally> selected = new ArrayList<>();
		int rank = 0;
		int previousStickerCount = Integer.MIN_VALUE;
		for (int index = 0; index < ordered.size(); index++) {
			VoteCandidateTally tally = ordered.get(index);
			if (tally.stickerCount() < cutoffStickerCount) {
				break;
			}
			if (tally.stickerCount() != previousStickerCount) {
				rank = index + 1;
				previousStickerCount = tally.stickerCount();
			}
			selected.add(tally.selectedAt(rank));
		}
		return List.copyOf(selected);
	}
}

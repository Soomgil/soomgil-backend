package com.soomgil.voting.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;

import com.soomgil.voting.domain.model.VoteNextScreen;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class VoteSessionPolicyTest {

	@Test
	@DisplayName("지급 개수와 선정 개수는 1 이상이고 후보 수를 넘을 수 없다")
	void validatesAllowanceAndSelectionAgainstCandidateCount() {
		assertThat(VoteSessionPolicy.isValidStickerAllowance(1, 10)).isTrue();
		assertThat(VoteSessionPolicy.isValidStickerAllowance(10, 10)).isTrue();
		assertThat(VoteSessionPolicy.isValidStickerAllowance(11, 10)).isFalse();
		assertThat(VoteSessionPolicy.isValidStickerAllowance(0, 10)).isFalse();

		assertThat(VoteSessionPolicy.isValidSelectionCount(3, 10)).isTrue();
		assertThat(VoteSessionPolicy.isValidSelectionCount(10, 10)).isTrue();
		assertThat(VoteSessionPolicy.isValidSelectionCount(11, 10)).isFalse();
		assertThat(VoteSessionPolicy.isValidSelectionCount(0, 10)).isFalse();
	}

	@Test
	@DisplayName("사용한 스티커 총합이 지급량을 넘으면 안 된다")
	void validatesTotalStickerUsage() {
		assertThat(VoteSessionPolicy.isWithinAllowance(5, 5)).isTrue();
		assertThat(VoteSessionPolicy.isWithinAllowance(3, 5)).isTrue();
		assertThat(VoteSessionPolicy.isWithinAllowance(0, 5)).isTrue();
		assertThat(VoteSessionPolicy.isWithinAllowance(6, 5)).isFalse();
	}

	@Test
	@DisplayName("한 후보에 붙이는 스티커 개수는 1 이상이어야 한다")
	void validatesPerCandidateStickerCount() {
		assertThat(VoteSessionPolicy.isValidPlacementCount(1)).isTrue();
		assertThat(VoteSessionPolicy.isValidPlacementCount(5)).isTrue();
		assertThat(VoteSessionPolicy.isValidPlacementCount(0)).isFalse();
		assertThat(VoteSessionPolicy.isValidPlacementCount(-1)).isFalse();
	}

	@Test
	@DisplayName("세션이 없으면 지도 화면으로 보낸다")
	void noSessionGoesToMap() {
		assertThat(VoteSessionPolicy.nextScreen(null, null)).isEqualTo(VoteNextScreen.MAP);
	}

	@Test
	@DisplayName("완료된 투표는 참여 여부와 관계없이 지도 화면으로 보낸다")
	void completedSessionGoesToMap() {
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.COMPLETED, VoteParticipantStatus.NOT_STARTED))
			.isEqualTo(VoteNextScreen.MAP);
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.COMPLETED, VoteParticipantStatus.SUBMITTED))
			.isEqualTo(VoteNextScreen.MAP);
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.COMPLETED, null))
			.isEqualTo(VoteNextScreen.MAP);
	}

	@Test
	@DisplayName("이미 완료된 투표에 나중에 들어온 신규 참여자는 지도로 보낸다")
	void newcomerAfterCompletionGoesToMap() {
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.COMPLETED, null))
			.isEqualTo(VoteNextScreen.MAP);
	}

	@Test
	@DisplayName("진행 중이지만 참여자가 아니면 지도로 보낸다")
	void nonParticipantOfOpenSessionGoesToMap() {
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.OPEN, null))
			.isEqualTo(VoteNextScreen.MAP);
	}

	@Test
	@DisplayName("아직 제출하지 않은 참여자는 지도보다 투표 화면을 먼저 본다")
	void unsubmittedParticipantSeesVoteScreen() {
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.OPEN, VoteParticipantStatus.NOT_STARTED))
			.isEqualTo(VoteNextScreen.VOTE);
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.OPEN, VoteParticipantStatus.IN_PROGRESS))
			.isEqualTo(VoteNextScreen.VOTE);
	}

	@Test
	@DisplayName("제출한 참여자는 투표가 끝날 때까지 대기 화면을 본다")
	void submittedParticipantWaits() {
		assertThat(VoteSessionPolicy.nextScreen(VoteSessionStatus.OPEN, VoteParticipantStatus.SUBMITTED))
			.isEqualTo(VoteNextScreen.WAITING);
	}

	@Test
	@DisplayName("모든 참여자가 제출하면 자동 종료 대상이다")
	void everyoneSubmittedTriggersAutoCompletion() {
		assertThat(VoteSessionPolicy.shouldAutoComplete(3, 3)).isTrue();
		assertThat(VoteSessionPolicy.shouldAutoComplete(2, 3)).isFalse();
		assertThat(VoteSessionPolicy.shouldAutoComplete(0, 0)).isFalse();
	}
}

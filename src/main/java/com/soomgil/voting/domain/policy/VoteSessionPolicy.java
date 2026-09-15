package com.soomgil.voting.domain.policy;

import com.soomgil.voting.domain.model.VoteNextScreen;
import com.soomgil.voting.domain.model.VoteParticipantStatus;
import com.soomgil.voting.domain.model.VoteSessionStatus;

/**
 * 투표 세션의 설정값 검증과 진입 화면 판정 규칙.
 *
 * <p>DB CHECK 제약과 별개로 application 계층에서 마지막으로 보장한다.
 */
public final class VoteSessionPolicy {

	/** 방장이 후보 수를 지정하지 않았을 때 사용하는 기본 후보 수. */
	public static final int DEFAULT_CANDIDATE_COUNT = 10;

	private VoteSessionPolicy() {
	}

	/**
	 * 사용자당 스티커 지급 개수가 유효한지 검사한다.
	 *
	 * @param stickerAllowance 지급 개수
	 * @param candidateCount 후보 관광지 수
	 * @return 1 이상이고 후보 수를 넘지 않으면 true
	 */
	public static boolean isValidStickerAllowance(int stickerAllowance, int candidateCount) {
		return stickerAllowance >= 1 && stickerAllowance <= candidateCount;
	}

	/**
	 * 최종 선정 관광지 개수가 유효한지 검사한다.
	 *
	 * @param selectionCount 선정 개수
	 * @param candidateCount 후보 관광지 수
	 * @return 1 이상이고 후보 수를 넘지 않으면 true
	 */
	public static boolean isValidSelectionCount(int selectionCount, int candidateCount) {
		return selectionCount >= 1 && selectionCount <= candidateCount;
	}

	/**
	 * 사용한 스티커 총합이 지급량 안에 있는지 검사한다.
	 *
	 * @param usedStickerCount 사용한 스티커 총합
	 * @param stickerAllowance 지급 개수
	 * @return 지급량을 넘지 않으면 true
	 */
	public static boolean isWithinAllowance(int usedStickerCount, int stickerAllowance) {
		return usedStickerCount >= 0 && usedStickerCount <= stickerAllowance;
	}

	/**
	 * 한 후보에 붙이는 스티커 개수가 유효한지 검사한다.
	 *
	 * <p>0개를 붙이는 것은 "붙이지 않음"이므로 배치 목록에 넣지 않고 제거해야 한다.
	 *
	 * @param stickerCount 후보 하나에 붙인 스티커 개수
	 * @return 1 이상이면 true
	 */
	public static boolean isValidPlacementCount(int stickerCount) {
		return stickerCount >= 1;
	}

	/**
	 * 여행 방 진입 시 먼저 보여줄 화면을 계산한다.
	 *
	 * <p>진행 중인 투표의 참여자만 투표/대기 화면을 본다. 완료된 투표, 세션이 없는 경우,
	 * 세션 시작 이후 합류해 참여자가 아닌 경우는 모두 지도 화면으로 보낸다.
	 *
	 * @param sessionStatus 현재 세션 상태. 세션이 없으면 null
	 * @param participantStatus 요청 사용자의 참여 상태. 참여자가 아니면 null
	 * @return 먼저 보여줄 화면
	 */
	public static VoteNextScreen nextScreen(
		VoteSessionStatus sessionStatus,
		VoteParticipantStatus participantStatus
	) {
		if (sessionStatus != VoteSessionStatus.OPEN || participantStatus == null) {
			return VoteNextScreen.MAP;
		}
		return participantStatus == VoteParticipantStatus.SUBMITTED
			? VoteNextScreen.WAITING
			: VoteNextScreen.VOTE;
	}

	/**
	 * 모든 활성 참여자가 제출을 마쳐 자동 종료해야 하는지 판단한다.
	 *
	 * @param submittedCount 제출을 마친 참여자 수
	 * @param totalCount 전체 참여자 수
	 * @return 참여자가 1명 이상이고 전원이 제출했으면 true
	 */
	public static boolean shouldAutoComplete(int submittedCount, int totalCount) {
		return totalCount > 0 && submittedCount >= totalCount;
	}
}

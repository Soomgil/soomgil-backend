package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;
import java.util.List;
import java.util.UUID;

/**
 * 투표에서 스티커를 붙인 장소들을 TRIP_VOTE 출처로 개인 취향에 반영하는 command.
 *
 * <p>다른 모듈이 preference mapper나 DB를 직접 수정하지 않고 취향을 반영할 때 사용하는 공개 계약이다.
 *
 * <p>일정 최종 선정 여부와 무관하게 사용자가 스티커를 붙인 모든 장소를 반영하며, 장소별 스티커 개수를 보존한다.
 * 근거 강도는 {@code TripVoteEvidencePolicy}가 결정하고 기존 projection 계산 공식은 변경하지 않는다.
 *
 * <p>{@code (voteSessionId, userId, provider, externalPlaceId)} 기준으로 멱등하다. 같은 제출을 재시도해도
 * 근거가 중복 반영되지 않는다.
 *
 * <p>이 command는 스와이프 최종 반응({@code user_place_reactions}), 스와이프 이벤트 로그,
 * 저장 장소를 변경하지 않는다. LIKE/NOPE/SUPER_LIKE 의미를 오염시키지 않기 위해서다.
 *
 * @param voteSessionId 멱등성 기준이 되는 투표 세션 식별자
 * @param userId 취향을 반영할 사용자
 * @param places 스티커를 붙인 장소 목록
 */
public record ApplyTripVotePreferenceCommand(
	UUID voteSessionId,
	UUID userId,
	List<TripVoteStickerPlace> places
) implements Command<ApplyTripVotePreferenceResult> {

	public ApplyTripVotePreferenceCommand {
		places = places == null ? List.of() : List.copyOf(places);
	}
}

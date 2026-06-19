package com.soomgil.social.application.query.dto;

import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.user.api.dto.UserSummary;

/**
 * 팔로우한 사용자가 특정 장소에 남긴 긍정 반응의 공개 표시 정보.
 *
 * <p>API에는 사용자 요약만 전달하고 반응 점수, 태그 가중치, 스와이프 이력은 포함하지 않는다.
 *
 * @param place 긍정 반응을 남긴 장소
 * @param followee 공개 가능한 팔로우 사용자 요약
 */
public record FolloweePlaceReaction(
	PlaceRef place,
	UserSummary followee
) {
}

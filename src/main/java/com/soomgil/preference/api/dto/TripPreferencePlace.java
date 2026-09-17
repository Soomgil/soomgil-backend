package com.soomgil.preference.api.dto;

/** 지도 범위 내 장소와 공개가 허용된 여행 멤버의 최종 긍정 반응. 점수나 싫어요는 노출하지 않는다. */
public record TripPreferencePlace(
    String provider, String externalPlaceId, String name, String address,
    Double lat, Double lng, String thumbnailUrl, String category,
    String userId, String displayName, String profileImageUrl, String reaction
) {}

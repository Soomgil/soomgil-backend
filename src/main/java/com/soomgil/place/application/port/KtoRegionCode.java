package com.soomgil.place.application.port;

/**
 * 한국관광공사 지역 필터 코드 쌍.
 *
 * @param areaCode KTO 시도 코드
 * @param sigunguCode KTO 시군구 코드. 시도 전체를 뜻하면 null
 */
public record KtoRegionCode(String areaCode, String sigunguCode) {
}

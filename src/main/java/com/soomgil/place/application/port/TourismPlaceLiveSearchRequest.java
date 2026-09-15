package com.soomgil.place.application.port;

/**
 * KTO 관광 API에서 즉시 장소를 조회하기 위한 요청.
 *
 * <p>{@code legalRegionCode}는 이름과 달리 KTO {@code areaCode}(관광공사 시도 코드)를 담는다. 법정동 코드는
 * 호출 전에 {@code LegalRegionKtoCodeResolver}가 KTO 코드로 바꾼다.
 *
 * @param q 검색어
 * @param bbox 지도 viewport 문자열
 * @param legalRegionCode KTO areaCode
 * @param category 관광지 분류
 * @param limit 최대 조회 수
 * @param sigunguCode KTO sigunguCode. areaCode 안에서 시군구로 좁힐 때만 쓰고 시도 전체면 null
 */
public record TourismPlaceLiveSearchRequest(
	String q,
	String bbox,
	String legalRegionCode,
	String category,
	int limit,
	String sigunguCode
) {
	/**
	 * 시군구 없이 만든다. 기존 호출자와의 호환용이다.
	 */
	public TourismPlaceLiveSearchRequest(String q, String bbox, String legalRegionCode, String category, int limit) {
		this(q, bbox, legalRegionCode, category, limit, null);
	}
}

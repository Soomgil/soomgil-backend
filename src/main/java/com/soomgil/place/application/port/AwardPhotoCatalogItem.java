package com.soomgil.place.application.port;

/**
 * 한국관광공사 관광사진 공모전 수상작 1건의 원천 metadata.
 *
 * <p>외부 API 응답을 그대로 정규화한 값이며 가공하지 않는다. 촬영지 문자열({@code filmLocation})은
 * {@code "경상남도 합천군, 가야산국립공원"}처럼 행정구역과 장소명을 쉼표로 이어붙인 형태로 내려온다.
 *
 * @param awardContentId 수상작 식별자. 공모전 API가 부여하며 목록 내에서 유일하다
 * @param title 작품명
 * @param filmLocation 촬영지 원문. 행정구역과 장소명이 쉼표로 이어져 있으며 null일 수 있다
 * @param photographer 촬영자 이름
 * @param awardDivision 수상 부문. 예: {@code "스마트폰 부문 [입선]"}
 * @param filmDay 촬영 시기. {@code yyyyMM} 형식이며 null일 수 있다
 * @param imageUrl 원본 이미지 URL
 * @param thumbnailUrl 썸네일 이미지 URL
 * @param copyrightCode 저작권 구분 코드. {@code Type1}은 출처 표시 조건이 붙는다
 * @param regionCode 촬영지 시도 코드. 빈 값으로 내려오는 항목이 있어 null일 수 있다
 * @param keywords 검색 키워드 원문. 장소 매칭 보조에 사용하며 null일 수 있다
 */
public record AwardPhotoCatalogItem(
	String awardContentId,
	String title,
	String filmLocation,
	String photographer,
	String awardDivision,
	String filmDay,
	String imageUrl,
	String thumbnailUrl,
	String copyrightCode,
	String regionCode,
	String keywords
) {
}

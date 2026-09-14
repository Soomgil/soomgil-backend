package com.soomgil.place.api.dto;

/**
 * 화면에 노출하는 관광사진 공모전 수상작 1건.
 *
 * <p>수상작 이미지는 저작권 구분이 {@code Type1}이라 출처 표시가 필요하다. 이미지를 표시하는 화면은
 * {@code photographer}와 {@code awardDivision}을 함께 노출해야 한다.
 *
 * @param awardContentId 수상작 식별자
 * @param title 작품명
 * @param placeName 촬영지에서 추출한 관광지명. 추출에 실패하면 null
 * @param regionName 촬영지에서 추출한 행정구역명. 추출에 실패하면 null
 * @param filmLocation 촬영지 원문
 * @param photographer 촬영자 이름. 출처 표시에 사용한다
 * @param awardDivision 수상 부문. 출처 표시에 사용한다
 * @param filmYearMonth 촬영 시기. {@code yyyy-MM} 형식이며 원문이 없으면 null
 * @param imageUrl 원본 이미지 URL
 * @param thumbnailUrl 썸네일 이미지 URL. 없으면 null
 * @param copyrightCode 저작권 구분 코드
 * @param regionCode 촬영지 시도 코드. 없으면 null
 */
public record AwardPhoto(
	String awardContentId,
	String title,
	String placeName,
	String regionName,
	String filmLocation,
	String photographer,
	String awardDivision,
	String filmYearMonth,
	String imageUrl,
	String thumbnailUrl,
	String copyrightCode,
	String regionCode
) {
}

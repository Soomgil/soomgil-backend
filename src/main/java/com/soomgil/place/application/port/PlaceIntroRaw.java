package com.soomgil.place.application.port;

/**
 * KTO detailIntro 응답의 raw 텍스트 필드를 담는 값 객체.
 * contentTypeId별로 필드명이 다르지만 동일한 필드 셋으로 정규화해서 보관한다.
 * 누락된 필드는 null. 빈 문자열은 null로 정규화한다.
 */
public record PlaceIntroRaw(
	String useTime,
	String restDate,
	String parking,
	String disability,
	String chkBabyCarriage,
	String chkPet
) {
	public static PlaceIntroRaw empty() {
		return new PlaceIntroRaw(null, null, null, null, null, null);
	}

	public boolean isEmpty() {
		return useTime == null
			&& restDate == null
			&& parking == null
			&& disability == null
			&& chkBabyCarriage == null
			&& chkPet == null;
	}
}

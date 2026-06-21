package com.soomgil.tourismsource.matching.infrastructure;

/**
 * 관광지 제목 매칭용 row.
 *
 * @param no 관광지 원천 PK
 * @param title 관광지명
 */
public record TourismSourceAttractionMatchRow(
	Integer no,
	String title
) {
}

package com.soomgil.preference.infrastructure.persistence.row;

/**
 * 태그 code로 preference tag를 찾은 결과 row.
 *
 * @param id preference tag id 문자열
 * @param code preference tag code
 */
public record PreferenceTagLookupRow(
	String id,
	String code
) {
}

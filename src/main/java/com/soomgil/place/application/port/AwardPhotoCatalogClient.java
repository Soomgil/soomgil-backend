package com.soomgil.place.application.port;

import java.util.List;

/**
 * 관광사진 공모전 수상작 목록을 조회하는 port.
 *
 * <p>수상작은 전체 규모가 100건 내외로 작아 목록 전체를 한 번에 받아 사용한다.
 * 구현체는 하루 단위 캐시를 유지하므로 호출자가 별도로 캐시하지 않는다.
 */
public interface AwardPhotoCatalogClient {

	/**
	 * 공개 가능한 수상작 전체를 조회한다.
	 *
	 * <p>이미지 URL이 없는 항목은 제외한다. 외부 API 호출에 실패하면 빈 목록을 반환하며 예외를 던지지 않는다.
	 *
	 * @return 수상작 목록. 조회 실패 시 빈 목록
	 */
	List<AwardPhotoCatalogItem> fetchCatalog();
}

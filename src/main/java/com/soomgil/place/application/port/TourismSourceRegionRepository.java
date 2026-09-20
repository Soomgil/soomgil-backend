package com.soomgil.place.application.port;

import java.util.Optional;

/**
 * 관광 원천 지역 코드 읽기 계약.
 *
 * <p>법정동 시군구 이름을 KTO 시군구 코드로 잇는 데 쓴다. 원천 시군구 목록이 아직 적재되지 않은 환경에서는
 * empty를 돌려주고, 호출자는 시도 범위로 넓혀 조회한다.
 */
public interface TourismSourceRegionRepository {
	/**
	 * 시도 안에서 이름이 같은 시군구의 KTO 코드를 찾는다. 공백 차이는 무시한다.
	 *
	 * @param sidoCode KTO 시도 코드
	 * @param gugunName 법정동 시군구 이름(예: 제주시)
	 * @return KTO 시군구 코드. 없으면 empty
	 */
	Optional<Integer> findGugunCode(int sidoCode, String gugunName);

	/**
	 * 지역(시도, 선택적 시군구)에 속한 관광 원천 장소들의 좌표 범위를 구한다.
	 *
	 * @param areaCode  KTO 시도 코드
	 * @param gugunCode KTO 시군구 코드. null이면 시도 전체
	 * @return 좌표가 있는 장소가 하나라도 있으면 그 범위, 없으면 empty
	 */
	Optional<RegionViewport> findRegionViewport(int areaCode, Integer gugunCode);
}

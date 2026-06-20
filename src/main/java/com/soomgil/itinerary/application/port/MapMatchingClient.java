package com.soomgil.itinerary.application.port;

/**
 * 외부 map matching provider client 계약.
 */
public interface MapMatchingClient {

	/**
	 * 입력 좌표를 provider 도로망에 맞춰 snapped route로 변환한다.
	 *
	 * @param request map matching 요청
	 * @return 성공 결과
	 */
	MapMatchClientResult match(MapMatchClientRequest request);
}

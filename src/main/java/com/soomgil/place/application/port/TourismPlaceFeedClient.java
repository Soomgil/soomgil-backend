package com.soomgil.place.application.port;

/**
 * 관광공사 원격 API에서 스와이프 장소 후보를 조회하는 port.
 */
public interface TourismPlaceFeedClient {

	TourismPlaceFeedResult fetch(TourismPlaceFeedRequest request);
}

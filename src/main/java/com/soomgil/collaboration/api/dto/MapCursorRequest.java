package com.soomgil.collaboration.api.dto;

/**
 * 지도 위 커서 좌표 전송 요청.
 *
 * @param longitude 경도
 * @param latitude 위도
 * @param sequence 해당 session의 단조 증가 sequence
 */
public record MapCursorRequest(double longitude, double latitude, long sequence) {
}

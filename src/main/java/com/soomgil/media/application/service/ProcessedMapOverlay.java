package com.soomgil.media.application.service;

/**
 * 서버 정제를 마친 지도 오버레이 이미지.
 *
 * @param bytes 메타데이터가 제거된 이미지 bytes
 * @param mimeType 최종 저장 MIME
 * @param width 최종 너비
 * @param height 최종 높이
 */
public record ProcessedMapOverlay(byte[] bytes, String mimeType, int width, int height) {
}

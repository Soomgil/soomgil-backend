package com.soomgil.collaboration.api.dto;

import java.util.UUID;

/**
 * 지도 오브젝트 편집 lease 변경 요청.
 *
 * @param drawingId 대상 지도 오브젝트 ID
 * @param action lease 동작
 */
public record MapObjectLockRequest(UUID drawingId, MapObjectLockAction action) {
}

package com.soomgil.itinerary.application.port;

import java.util.UUID;

/** IMAGE 지도 오브젝트가 여행방에 연결된 활성 MAP_OVERLAY 미디어만 참조하도록 검사하는 경계. */
@FunctionalInterface
public interface MapOverlayMediaAccess {

	boolean canUse(UUID tripId, UUID mediaFileId);
}

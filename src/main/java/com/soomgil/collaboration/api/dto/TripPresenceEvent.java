package com.soomgil.collaboration.api.dto;

import java.util.List;
import java.util.UUID;

public record TripPresenceEvent(
	String eventType,
	UUID tripId,
	List<UUID> activeUserIds
) {

	public static TripPresenceEvent snapshot(UUID tripId, List<UUID> activeUserIds) {
		return new TripPresenceEvent("presence.snapshot", tripId, List.copyOf(activeUserIds));
	}
}

package com.soomgil.media.application;

import com.soomgil.itinerary.application.port.MapOverlayMediaAccess;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.domain.model.MediaFileMetadata;
import com.soomgil.media.domain.model.MediaPurpose;
import com.soomgil.media.domain.policy.MediaObjectKeyPolicy;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** media metadata를 기준으로 여행방 MAP_OVERLAY 참조 가능 여부를 제공한다. */
@Component
public class MapOverlayMediaAccessAdapter implements MapOverlayMediaAccess {

	private final MediaFileRepository repository;
	private final MediaObjectKeyPolicy keyPolicy;

	public MapOverlayMediaAccessAdapter(MediaFileRepository repository, MediaObjectKeyPolicy keyPolicy) {
		this.repository = Objects.requireNonNull(repository, "repository must not be null");
		this.keyPolicy = Objects.requireNonNull(keyPolicy, "keyPolicy must not be null");
	}

	@Override
	public boolean canUse(UUID tripId, UUID mediaFileId) {
		MediaFileMetadata media = mediaFileId == null ? null : repository.findById(mediaFileId);
		return media != null
			&& "ACTIVE".equals(media.status())
			&& "TRIP".equals(media.linkedResourceType())
			&& tripId.equals(media.linkedResourceId())
			&& keyPolicy.requireOwnedPurpose(media.ownerUserId(), media.objectKey()) == MediaPurpose.MAP_OVERLAY;
	}
}

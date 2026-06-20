package com.soomgil.media.infrastructure.persistence.repository;

import com.soomgil.media.application.port.LinkedMediaResourceAuthorizer;
import com.soomgil.media.infrastructure.persistence.mapper.MediaFileMapper;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** profile 본인 여부와 active 여행 멤버의 기록 접근 권한을 SQL로 확인한다. */
@Component
public class SqlLinkedMediaResourceAuthorizer implements LinkedMediaResourceAuthorizer {

	private final MediaFileMapper mapper;

	public SqlLinkedMediaResourceAuthorizer(MediaFileMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public boolean canLink(UUID userId, String resourceType, UUID resourceId) {
		return switch (resourceType) {
			case "USER_PROFILE" -> userId.equals(resourceId);
			case "TRIP_RECORD" -> mapper.countAccessibleTripRecord(userId, resourceId) > 0;
			default -> false;
		};
	}
}

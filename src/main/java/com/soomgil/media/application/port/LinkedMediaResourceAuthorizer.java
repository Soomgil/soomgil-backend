package com.soomgil.media.application.port;

import java.util.UUID;

/** media metadata를 특정 profile, 기록, 게시물에 연결할 권한을 확인하는 계약. */
@FunctionalInterface
public interface LinkedMediaResourceAuthorizer {

	boolean canLink(UUID userId, String resourceType, UUID resourceId);
}

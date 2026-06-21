package com.soomgil.social.infrastructure.persistence;

import java.util.UUID;

public record UserSummaryRecord(
	UUID id,
	String displayName,
	String profileImageUrl
) {
}

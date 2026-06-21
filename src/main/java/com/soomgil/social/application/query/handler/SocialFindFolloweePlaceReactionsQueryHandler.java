package com.soomgil.social.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.social.application.query.dto.FindFolloweePlaceReactionsQuery;
import com.soomgil.social.application.query.dto.FolloweePlaceReaction;
import com.soomgil.social.infrastructure.persistence.mapper.SocialFolloweePlaceReactionMapper;
import com.soomgil.social.infrastructure.persistence.row.FolloweePlaceReactionRow;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 활성 팔로우 관계와 긍정 장소 반응을 조합해 공개 가능한 사용자 요약을 반환한다.
 */
@Service
public class SocialFindFolloweePlaceReactionsQueryHandler implements FindFolloweePlaceReactionsQueryHandler {

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final SocialFolloweePlaceReactionMapper mapper;

	public SocialFindFolloweePlaceReactionsQueryHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		SocialFolloweePlaceReactionMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
	}

	@Transactional(readOnly = true)
	@Override
	public List<FolloweePlaceReaction> handle(FindFolloweePlaceReactionsQuery query) {
		if (query.places().isEmpty()) {
			return List.of();
		}

		return mapper.findPositiveReactions(currentUserId().toString(), query.places())
			.stream()
			.map(this::toReaction)
			.toList();
	}

	private UUID currentUserId() {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to read followee reactions.");
		}
		return provider.currentUserId();
	}

	private FolloweePlaceReaction toReaction(FolloweePlaceReactionRow row) {
		return new FolloweePlaceReaction(
			new PlaceRef(PlaceProvider.valueOf(row.provider()), row.externalPlaceId()),
			new UserSummary(UUID.fromString(row.userId()), row.displayName(), toUri(row.profileImageUrl()))
		);
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}
}

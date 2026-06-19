package com.soomgil.preference.application.command.handler;

import com.soomgil.common.id.Ids;
import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionInsertRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionUpdateRow;
import com.soomgil.preference.infrastructure.persistence.row.UserSwipeEventInsertRow;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 사용자의 장소 스와이프 최종 반응과 이벤트 로그를 저장한다.
 */
@Service
public class PreferenceUpsertSwipeReactionCommandHandler implements UpsertSwipeReactionCommandHandler {

	private final ObjectProvider<CurrentUserProvider> currentUserProvider;
	private final PreferenceSwipeReactionMapper mapper;

	public PreferenceUpsertSwipeReactionCommandHandler(
		ObjectProvider<CurrentUserProvider> currentUserProvider,
		PreferenceSwipeReactionMapper mapper
	) {
		this.currentUserProvider = currentUserProvider;
		this.mapper = mapper;
	}

	@Transactional
	@Override
	public SwipeReactionResponse handle(UpsertSwipeReactionCommand command) {
		CurrentUserProvider provider = currentUserProvider.getIfAvailable();
		if (provider == null) {
			throw new IllegalStateException("CurrentUserProvider is required to upsert swipe reactions.");
		}

		UUID userId = provider.currentUserId();
		String placeProvider = command.provider().name();
		String reaction = command.reaction().name();
		UserPlaceReactionRow previous = mapper.findReaction(
			userId.toString(),
			placeProvider,
			command.externalPlaceId()
		);

		if (previous == null) {
			mapper.insertReaction(new UserPlaceReactionInsertRow(
				Ids.newUuid().toString(),
				userId.toString(),
				placeProvider,
				command.externalPlaceId(),
				reaction,
				command.sourceModifiedAt()
			));
		}
		else {
			mapper.updateReaction(new UserPlaceReactionUpdateRow(
				previous.id(),
				reaction,
				command.sourceModifiedAt()
			));
		}

		mapper.insertEvent(new UserSwipeEventInsertRow(
			userId.toString(),
			placeProvider,
			command.externalPlaceId(),
			reaction,
			previous == null ? null : previous.reaction(),
			command.sourceModifiedAt()
		));

		return new SwipeReactionResponse(
			new PlaceRef(command.provider(), command.externalPlaceId()),
			command.reaction(),
			command.reaction() == SwipeReaction.SUPER_LIKE,
			OffsetDateTime.now()
		);
	}
}

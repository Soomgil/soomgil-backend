package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import com.soomgil.community.api.dto.ModerationStatus;
import com.soomgil.community.api.dto.PostVisibility;
import com.soomgil.community.domain.model.CommunityPostRecord;
import com.soomgil.community.infrastructure.persistence.mapper.CommunityPostMapper;
import com.soomgil.community.infrastructure.persistence.mapper.PostRetripMapper;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.trip.application.port.TripCommandRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetripCommunityPostServiceTest {

	@Test
	void createsAnOwnerTripFromTheStoredPostSnapshot() {
		CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
		PostRetripMapper retripMapper = mock(PostRetripMapper.class);
		TripCommandRepository tripRepository = mock(TripCommandRepository.class);
		ItineraryCommandRepository itineraryRepository = mock(ItineraryCommandRepository.class);
		FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		String snapshotJson = codec.encode(new CommunityPostSnapshot(List.of(), List.of(), null));
		when(postMapper.findById(postId)).thenReturn(Optional.of(new CommunityPostRecord(
			postId, UUID.randomUUID(), 4L, UUID.randomUUID(), PostVisibility.PUBLIC,
			"원본 여행", null, null, 1, null, null, null, ModerationStatus.VISIBLE,
			Instant.now(), null, snapshotJson
		)));
		when(displayNameHandler.handle(any())).thenReturn("민경철");

		RetripCommunityPostService service = new RetripCommunityPostService(
			postMapper, retripMapper, tripRepository, itineraryRepository, codec, new ObjectMapper(), displayNameHandler
		);
		var result = service.retrip(postId, userId, null);

		assertThat(result.retrippedFromPostId()).isEqualTo(postId);
		assertThat(result.ownerUserId()).isEqualTo(userId);
		assertThat(result.members().getFirst().user().displayName()).isEqualTo("민경철");
		verify(tripRepository).saveCreatedRetrip(any(), any(), org.mockito.ArgumentMatchers.eq(postId), org.mockito.ArgumentMatchers.eq(1));
		verify(retripMapper).insert(any(), org.mockito.ArgumentMatchers.eq(postId), org.mockito.ArgumentMatchers.eq(userId), any(), org.mockito.ArgumentMatchers.eq(1), any());
	}
}

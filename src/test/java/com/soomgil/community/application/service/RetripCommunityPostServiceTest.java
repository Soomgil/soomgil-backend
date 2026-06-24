package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
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
import com.soomgil.itinerary.api.dto.GeometryFormat;
import com.soomgil.itinerary.api.dto.ItineraryDay;
import com.soomgil.itinerary.api.dto.ItineraryDayGroupType;
import com.soomgil.itinerary.api.dto.ItineraryItem;
import com.soomgil.itinerary.api.dto.ItineraryItemType;
import com.soomgil.itinerary.api.dto.RouteMode;
import com.soomgil.itinerary.api.dto.RouteSegment;
import com.soomgil.itinerary.application.port.ItineraryCommandRepository;
import com.soomgil.itinerary.application.port.ItineraryDayCreate;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.api.dto.ChecklistItem;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.api.dto.PlanningScopeType;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.NoteMapper;
import com.soomgil.trip.application.port.TripCommandRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RetripCommunityPostServiceTest {

	@Test
	void createsAnOwnerTripFromTheStoredPostSnapshot() {
		CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
		PostRetripMapper retripMapper = mock(PostRetripMapper.class);
		TripCommandRepository tripRepository = mock(TripCommandRepository.class);
		ItineraryCommandRepository itineraryRepository = mock(ItineraryCommandRepository.class);
		NoteMapper noteMapper = mock(NoteMapper.class);
		ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
		ChecklistItemMapper checklistItemMapper = mock(ChecklistItemMapper.class);
		FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		String snapshotJson = codec.encode(new CommunityPostSnapshot(List.of(), List.of(), List.of(), List.of(), null));
		when(postMapper.findById(postId)).thenReturn(Optional.of(new CommunityPostRecord(
			postId, UUID.randomUUID(), 4L, UUID.randomUUID(), PostVisibility.PUBLIC,
			"원본 여행", null, null, 1, null, null, null, ModerationStatus.VISIBLE,
			Instant.now(), null, snapshotJson
		)));
		when(displayNameHandler.handle(any())).thenReturn("민경철");
		when(itineraryRepository.incrementItineraryVersion(any(), eq(0L), any()))
			.thenReturn(OptionalLong.of(1L));

		RetripCommunityPostService service = new RetripCommunityPostService(
			postMapper, retripMapper, tripRepository, itineraryRepository,
			noteMapper, checklistMapper, checklistItemMapper,
			codec, new ObjectMapper(), displayNameHandler
		);
		var result = service.retrip(postId, userId, null);

		assertThat(result.retrippedFromPostId()).isEqualTo(postId);
		assertThat(result.ownerUserId()).isEqualTo(userId);
		assertThat(result.members().getFirst().user().displayName()).isEqualTo("민경철");
		verify(tripRepository).saveCreatedRetrip(any(), any(), org.mockito.ArgumentMatchers.eq(postId), org.mockito.ArgumentMatchers.eq(1));
		verify(retripMapper).insert(any(), org.mockito.ArgumentMatchers.eq(postId), org.mockito.ArgumentMatchers.eq(userId), any(), org.mockito.ArgumentMatchers.eq(1), any());
	}

	@Test
	void copiesItineraryNotesAndTodosFromTheStoredPostSnapshot() {
		CommunityPostMapper postMapper = mock(CommunityPostMapper.class);
		PostRetripMapper retripMapper = mock(PostRetripMapper.class);
		TripCommandRepository tripRepository = mock(TripCommandRepository.class);
		ItineraryCommandRepository itineraryRepository = mock(ItineraryCommandRepository.class);
		NoteMapper noteMapper = mock(NoteMapper.class);
		ChecklistMapper checklistMapper = mock(ChecklistMapper.class);
		ChecklistItemMapper checklistItemMapper = mock(ChecklistItemMapper.class);
		FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());
		UUID postId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		UUID sourceDayId = UUID.randomUUID();
		UUID sourceDay2Id = UUID.randomUUID();
		UUID sourceItemAId = UUID.randomUUID();
		UUID sourceItemBId = UUID.randomUUID();
		UUID sourceItemCId = UUID.randomUUID();
		var snapshot = new CommunityPostSnapshot(
			List.of(
				new ItineraryDay(
					sourceDayId, UUID.randomUUID(), ItineraryDayGroupType.DAY, 1, null, "첫째 날", 1,
					List.of(
						new ItineraryItem(
							sourceItemAId, sourceDayId, 1, ItineraryItemType.PLACE, null,
							"성심당", "대전 중구", 36.327, 127.427, null, PlaceSourceStatus.AVAILABLE
						),
						new ItineraryItem(
							sourceItemBId, sourceDayId, 2, ItineraryItemType.PLACE, null,
							"한밭수목원", "대전 서구", 36.368, 127.388, null, PlaceSourceStatus.AVAILABLE
						)
					)
				),
				new ItineraryDay(
					sourceDay2Id, UUID.randomUUID(), ItineraryDayGroupType.DAY, 2,
					LocalDate.of(2024, 1, 7), "둘째 날", 2,
					List.of(
						new ItineraryItem(
							sourceItemCId, sourceDay2Id, 1, ItineraryItemType.PLACE, null,
							"대전시립미술관", "대전 서구", 36.366, 127.385, null, PlaceSourceStatus.AVAILABLE
						)
					)
				)
			),
			List.of(new RouteSegment(
				UUID.randomUUID(), sourceItemAId, sourceItemBId, RouteMode.DRIVING, "OSRM",
				"car", GeometryFormat.GEOJSON, Map.of("type", "LineString", "coordinates", List.of()),
				1200.0, 600.0, 0.9
			)),
			List.of(
				new Note(UUID.randomUUID(), UUID.randomUUID(), PlanningScopeType.TRIP, null, "전체 메모", null),
				new Note(UUID.randomUUID(), UUID.randomUUID(), PlanningScopeType.DAY, sourceDayId, "첫째 날 메모", null)
			),
			List.of(new Checklist(
				UUID.randomUUID(), UUID.randomUUID(), PlanningScopeType.DAY, sourceDayId, "준비물",
				List.of(new ChecklistItem(UUID.randomUUID(), UUID.randomUUID(), 1, "우산 챙기기", List.of(), null))
			)),
			null
		);
		when(postMapper.findById(postId)).thenReturn(Optional.of(new CommunityPostRecord(
			postId, UUID.randomUUID(), 4L, UUID.randomUUID(), PostVisibility.PUBLIC,
			"원본 여행", null, null, 1, null, null, null, ModerationStatus.VISIBLE,
			Instant.now(), null, codec.encode(snapshot)
		)));
		when(itineraryRepository.incrementItineraryVersion(any(), eq(0L), any()))
			.thenReturn(OptionalLong.of(1L));
		when(displayNameHandler.handle(any())).thenReturn("민경철");

		RetripCommunityPostService service = new RetripCommunityPostService(
			postMapper, retripMapper, tripRepository, itineraryRepository,
			noteMapper, checklistMapper, checklistItemMapper,
			codec, new ObjectMapper(), displayNameHandler,
			Clock.fixed(Instant.parse("2026-06-25T00:00:00Z"), ZoneId.of("Asia/Seoul"))
		);

		var result = service.retrip(postId, userId, null);

		assertThat(result.itineraryVersion()).isEqualTo(1L);
		ArgumentCaptor<ItineraryDayCreate> dayCaptor = ArgumentCaptor.forClass(ItineraryDayCreate.class);
		verify(itineraryRepository, times(3)).insertDay(dayCaptor.capture());
		assertThat(dayCaptor.getAllValues())
			.extracting(day -> day.groupType().name())
			.containsExactly("DAY", "DAY", "UNSCHEDULED");
		assertThat(dayCaptor.getAllValues())
			.extracting(ItineraryDayCreate::date)
			.containsExactly(LocalDate.of(2026, 6, 25), LocalDate.of(2026, 6, 26), null);
		verify(itineraryRepository, times(3)).insertItem(any());
		verify(itineraryRepository).insertRouteSegment(any());
		verify(noteMapper, times(2)).insert(any(), any(), any(), any(), any(), eq(userId), any());
		verify(checklistMapper).insert(any(), any(), eq(PlanningScopeType.DAY), any(), eq("준비물"), eq(userId), any());
		verify(checklistItemMapper).insert(any(), any(), eq(1), eq("우산 챙기기"), eq(userId), any());
	}
}

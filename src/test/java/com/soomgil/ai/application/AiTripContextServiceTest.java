package com.soomgil.ai.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.place.application.service.PlaceAccessibilityCacheService;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.ListChecklistsQueryHandler;
import com.soomgil.planning.application.query.ListChecklistsQuery;
import com.soomgil.trip.application.query.dto.FindTripDetailQuery;
import com.soomgil.trip.application.query.dto.TripDetailView;
import com.soomgil.trip.application.query.dto.TripMemberView;
import com.soomgil.trip.application.query.handler.FindTripDetailHandler;
import com.soomgil.trip.domain.model.TripAccessRole;
import com.soomgil.trip.domain.model.TripMemberRole;
import com.soomgil.trip.domain.model.TripMemberStatus;
import com.soomgil.trip.domain.model.TripStatus;
import com.soomgil.user.api.dto.UserSummary;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import java.util.Optional;

class AiTripContextServiceTest {

	@Test
	void assemblesPublicTripMemberContextWithoutPreferenceInternals() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		FindTripDetailHandler tripHandler = mock(FindTripDetailHandler.class);
		FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
		GetNoteQueryHandler noteHandler = mock(GetNoteQueryHandler.class);
		ListChecklistsQueryHandler checklistHandler = mock(ListChecklistsQueryHandler.class);
		FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
		PlaceAccessibilityCacheService accessibilityCacheService = mock(PlaceAccessibilityCacheService.class);
		TripMemberView member = new TripMemberView(
			UUID.randomUUID(), tripId, userId, TripMemberRole.MEMBER,
			TripAccessRole.OWNER, TripMemberStatus.ACTIVE, Instant.now()
		);
		when(tripHandler.handle(new FindTripDetailQuery(tripId, userId))).thenReturn(new TripDetailView(
			tripId, "제주 여름 여행", "제주", TripStatus.ACTIVE, TripAccessRole.OWNER,
			7L, Instant.now(), userId, List.of(member), null
		));
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId))).thenReturn(new ItineraryView(
			tripId, 7L, List.of(), List.of(), List.of()
		));
		when(displayNameHandler.handle(new FindDisplayNameQuery(userId))).thenReturn("윤정");
		when(checklistHandler.handle(new ListChecklistsQuery(tripId, null, null, userId)))
			.thenReturn(List.of());
		when(noteHandler.findOptional(org.mockito.ArgumentMatchers.any()))
			.thenReturn(Optional.empty());
		when(accessibilityCacheService.getMany(org.mockito.ArgumentMatchers.any()))
			.thenReturn(Map.of());

		AiTripContext context = new AiTripContextService(
			tripHandler, itineraryHandler, noteHandler, checklistHandler, displayNameHandler,
			accessibilityCacheService
		).load(tripId, userId);

		assertThat(context.trip().title()).isEqualTo("제주 여름 여행");
		assertThat(context.members()).extracting(AiTripContext.MemberSummary::displayName).containsExactly("윤정");
		assertThat(context.notes()).isEmpty();
	}
}

package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.community.domain.model.CommunityException;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.dto.ItineraryView;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.planning.application.handler.GetNoteQueryHandler;
import com.soomgil.planning.application.handler.ListChecklistsQueryHandler;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TripItinerarySnapshotCheckerTest {

	private final TripAccessGuard accessGuard = mock(TripAccessGuard.class);
	private final FindItineraryHandler itineraryHandler = mock(FindItineraryHandler.class);
	private final GetNoteQueryHandler noteQueryHandler = mock(GetNoteQueryHandler.class);
	private final ListChecklistsQueryHandler checklistsQueryHandler = mock(ListChecklistsQueryHandler.class);
	private final FindDisplayNameQueryHandler displayNameHandler = mock(FindDisplayNameQueryHandler.class);
	private final TripItinerarySnapshotChecker checker = new TripItinerarySnapshotChecker(
		accessGuard, itineraryHandler, noteQueryHandler, checklistsQueryHandler, displayNameHandler
	);

	@Test
	void verifiesMembershipAndBuildsTheRequestedVersionSnapshot() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId)))
			.thenReturn(new ItineraryView(tripId, 7L, List.of(), List.of(), List.of()));
		when(noteQueryHandler.findOptional(org.mockito.ArgumentMatchers.any())).thenReturn(Optional.empty());
		when(checklistsQueryHandler.handle(org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
		when(displayNameHandler.handle(new FindDisplayNameQuery(userId))).thenReturn("민경철");

		var snapshot = checker.fetchSnapshot(tripId, 7L, userId);

		assertThat(snapshot.days()).isEmpty();
		assertThat(snapshot.notes()).isEmpty();
		assertThat(snapshot.checklists()).isEmpty();
		assertThat(snapshot.authorDisplay().displayName()).isEqualTo("민경철");
		verify(accessGuard).requireActiveMember(tripId, userId);
	}

	@Test
	void rejectsAStaleSourceVersion() {
		UUID tripId = UUID.randomUUID();
		UUID userId = UUID.randomUUID();
		when(itineraryHandler.handle(new FindItineraryQuery(tripId, userId)))
			.thenReturn(new ItineraryView(tripId, 8L, List.of(), List.of(), List.of()));

		assertThatThrownBy(() -> checker.fetchSnapshot(tripId, 7L, userId))
			.isInstanceOf(CommunityException.class);
	}
}

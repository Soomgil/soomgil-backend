package com.soomgil.collaboration.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.soomgil.collaboration.api.dto.CollaborationActionResponse;
import com.soomgil.collaboration.api.dto.UndoRedoRequest;
import com.soomgil.collaboration.application.command.dto.UndoRedoResult;
import com.soomgil.collaboration.application.command.handler.UndoRedoHandler;
import java.security.Principal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CollaborationControllerTest {

	private static final UUID TRIP_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");

	@Test
	void returnsUndoResponse() {
		UndoRedoHandler handler = mock(UndoRedoHandler.class);
		when(handler.handle(any())).thenReturn(new UndoRedoResult(TRIP_ID, 11L, 2L, false, true));
		CollaborationController controller = new CollaborationController(handler);

		CollaborationActionResponse response = controller.undo(
			TRIP_ID,
			new UndoRedoRequest(10L, null),
			" session-1 ",
			principal()
		);

		assertThat(response.tripId()).isEqualTo(TRIP_ID);
		assertThat(response.itineraryVersion()).isEqualTo(11L);
		assertThat(response.commandEventId()).isEqualTo(2L);
		assertThat(response.undoAvailable()).isFalse();
		assertThat(response.redoAvailable()).isTrue();
	}

	@Test
	void returnsRedoResponse() {
		UndoRedoHandler handler = mock(UndoRedoHandler.class);
		when(handler.handle(any())).thenReturn(new UndoRedoResult(TRIP_ID, 12L, 4L, true, false));
		CollaborationController controller = new CollaborationController(handler);

		CollaborationActionResponse response = controller.redo(
			TRIP_ID,
			new UndoRedoRequest(11L, 3L),
			"session-1",
			principal()
		);

		assertThat(response.itineraryVersion()).isEqualTo(12L);
		assertThat(response.commandEventId()).isEqualTo(4L);
		assertThat(response.undoAvailable()).isTrue();
		assertThat(response.redoAvailable()).isFalse();
	}

	private Principal principal() {
		return () -> USER_ID.toString();
	}
}

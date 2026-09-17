package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import com.soomgil.preference.infrastructure.persistence.row.UserPlaceReactionRow;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GetMyPlaceReactionHandlerTest {
    @Test
    @SuppressWarnings("unchecked")
    void readsOnlyCurrentUsersReactionAndReturnsNullWhenAbsent() {
        ObjectProvider<CurrentUserProvider> users = mock(ObjectProvider.class);
        var user = mock(CurrentUserProvider.class);
        var mapper = mock(PreferenceSwipeReactionMapper.class);
        var id = UUID.randomUUID();
        when(users.getObject()).thenReturn(user);
        when(user.currentUserId()).thenReturn(id);
        when(mapper.findReaction(id.toString(), "KTO", "1"))
            .thenReturn(new UserPlaceReactionRow("r", "SUPER_LIKE", null));
        var handler = new GetMyPlaceReactionHandler(users, mapper);
        assertEquals(SwipeReaction.SUPER_LIKE, handler.handle(PlaceProvider.KTO, "1"));
        assertNull(handler.handle(PlaceProvider.KTO, "2"));
        verify(mapper).findReaction(id.toString(), "KTO", "1");
        verify(mapper).findReaction(id.toString(), "KTO", "2");
    }
}

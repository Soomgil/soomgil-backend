package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.preference.infrastructure.persistence.mapper.TripPreferencePlaceMapper;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListTripPreferencePlacesHandlerTest {
    @Test
    @SuppressWarnings("unchecked")
    void checksTripAccessBeforeReadingAndRejectsInvalidBounds() {
        ObjectProvider<CurrentUserProvider> users = mock(ObjectProvider.class);
        var user = mock(CurrentUserProvider.class);
        when(users.getObject()).thenReturn(user);
        when(user.currentUserId()).thenReturn(UUID.randomUUID());
        var members = mock(ListTripMembersHandler.class);
        var mapper = mock(TripPreferencePlaceMapper.class);
        var handler = new ListTripPreferencePlacesHandler(users, members, mapper);
        assertEquals(ErrorCode.INVALID_REQUEST, assertThrows(BusinessException.class,
            () -> handler.handle(UUID.randomUUID(), "NaN,0,1,1")).errorCode());
        verifyNoInteractions(mapper, members);
        when(members.handle(any())).thenThrow(new IllegalStateException("Forbidden"));
        assertThrows(IllegalStateException.class, () -> handler.handle(UUID.randomUUID(), "126,33,127,34"));
        verifyNoInteractions(mapper);
    }

    @Test
    @SuppressWarnings("unchecked")
    void emptyMembershipNeverQueriesPreferences() {
        ObjectProvider<CurrentUserProvider> users = mock(ObjectProvider.class);
        var user = mock(CurrentUserProvider.class);
        when(users.getObject()).thenReturn(user);
        when(user.currentUserId()).thenReturn(UUID.randomUUID());
        var members = mock(ListTripMembersHandler.class);
        when(members.handle(any())).thenReturn(List.of());
        var mapper = mock(TripPreferencePlaceMapper.class);
        assertTrue(new ListTripPreferencePlacesHandler(users, members, mapper)
            .handle(UUID.randomUUID(), "126,33,127,34").isEmpty());
        verifyNoInteractions(mapper);
    }
}

package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.infrastructure.persistence.mapper.PreferenceSwipeReactionMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 인증한 사용자 자신의 장소 반응만 조회한다. 반응하지 않은 장소는 null을 반환한다. */
@Service
public class GetMyPlaceReactionHandler {
    private final ObjectProvider<CurrentUserProvider> users;
    private final PreferenceSwipeReactionMapper mapper;

    public GetMyPlaceReactionHandler(ObjectProvider<CurrentUserProvider> users, PreferenceSwipeReactionMapper mapper) {
        this.users = users;
        this.mapper = mapper;
    }

    /** 현재 인증 사용자와 장소 식별자로 저장된 최종 반응을 조회한다. */
    @Transactional(readOnly = true)
    public SwipeReaction handle(PlaceProvider provider, String externalPlaceId) {
        var user = users.getObject().currentUserId();
        var row = mapper.findReaction(user.toString(), provider.name(), externalPlaceId);
        return row == null ? null : SwipeReaction.valueOf(row.reaction());
    }
}

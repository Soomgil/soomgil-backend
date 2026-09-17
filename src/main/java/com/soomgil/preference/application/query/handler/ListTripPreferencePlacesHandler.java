package com.soomgil.preference.application.query.handler;

import com.soomgil.global.security.CurrentUserProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.preference.api.dto.TripPreferencePlace;
import com.soomgil.preference.infrastructure.persistence.mapper.TripPreferencePlaceMapper;
import com.soomgil.trip.application.query.dto.ListTripMembersQuery;
import com.soomgil.trip.application.query.handler.ListTripMembersHandler;
import com.soomgil.trip.domain.model.TripMemberStatus;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 여행방 참여 권한을 확인한 뒤 현재 지도 범위의 실제 좋아요·슈퍼라이크를 반환한다. */
@Service
public class ListTripPreferencePlacesHandler {
    private final ObjectProvider<CurrentUserProvider> users;
    private final ListTripMembersHandler members;
    private final TripPreferencePlaceMapper mapper;
    public ListTripPreferencePlacesHandler(ObjectProvider<CurrentUserProvider> users,
        ListTripMembersHandler members, TripPreferencePlaceMapper mapper) {
        this.users = users; this.members = members; this.mapper = mapper;
    }
    /** bbox는 서쪽,남쪽,동쪽,북쪽 순서의 경위도이며 날짜변경선을 가로지르지 않는다. */
    @Transactional(readOnly = true)
    public List<TripPreferencePlace> handle(UUID tripId, String bbox) {
        double[] b;
        try { b = Arrays.stream(bbox.split(",", -1)).mapToDouble(Double::parseDouble).toArray(); }
        catch (RuntimeException e) { throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid bbox"); }
        if (b.length != 4 || Arrays.stream(b).anyMatch(v -> !Double.isFinite(v))
            || b[0] < -180 || b[2] > 180 || b[1] < -90 || b[3] > 90 || b[0] >= b[2] || b[1] >= b[3]) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid bbox");
        }
        var viewer = users.getObject().currentUserId();
        var active = members.handle(new ListTripMembersQuery(tripId, viewer, TripMemberStatus.ACTIVE));
        if (active.isEmpty()) return List.of();
        return mapper.find(viewer.toString(), active.stream().map(m -> m.userId().toString()).toList(), b[0], b[1], b[2], b[3]);
    }
}

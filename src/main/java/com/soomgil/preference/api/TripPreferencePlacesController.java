package com.soomgil.preference.api;

import com.soomgil.preference.api.dto.TripPreferencePlace;
import com.soomgil.preference.application.query.handler.ListTripPreferencePlacesHandler;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

/** 여행방 지도에서 공개 가능한 선호 관광지를 조회하는 읽기 전용 API. */
@RestController
@RequestMapping("/api/v1/trips/{tripId}/preference-places")
public class TripPreferencePlacesController {
    private final ListTripPreferencePlacesHandler handler;
    public TripPreferencePlacesController(ListTripPreferencePlacesHandler handler) { this.handler = handler; }
    /** 현재 지도 영역의 최대 200개 장소를 장소별 멤버 반응 행으로 반환한다. */
    @GetMapping
    public List<TripPreferencePlace> list(@PathVariable UUID tripId, @RequestParam String bbox) {
        return handler.handle(tripId, bbox);
    }
}

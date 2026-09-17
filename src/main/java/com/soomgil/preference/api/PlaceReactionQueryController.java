package com.soomgil.preference.api;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.preference.api.dto.SwipeReaction;
import com.soomgil.preference.application.query.handler.GetMyPlaceReactionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 장소 상세에서 현재 사용자의 저장된 반응을 복원하는 조회 API. */
@RestController
@RequestMapping("/api/v1/places")
public class PlaceReactionQueryController {
    private final GetMyPlaceReactionHandler handler;

    public PlaceReactionQueryController(GetMyPlaceReactionHandler handler) { this.handler = handler; }

    /** 반응이 없으면 reaction 필드는 null이다. 다른 사용자의 반응은 노출하지 않는다. */
    @GetMapping("/{provider}/{externalPlaceId}/swipe-reaction")
    public MyReaction get(@PathVariable PlaceProvider provider, @PathVariable String externalPlaceId) {
        return new MyReaction(handler.handle(provider, externalPlaceId));
    }

    /** @param reaction 현재 사용자의 최종 반응. 미선택은 null. */
    public record MyReaction(SwipeReaction reaction) {}
}

package com.soomgil.place.application.port;

import java.util.List;

/** 취향 피드의 범위와 이미 반응해 제외할 장소 ID. */
public record TourismPlaceFeedRequest(String legalRegionCode,String category,int limit,String seed,List<String> excludedPlaceIds) {
    public TourismPlaceFeedRequest { excludedPlaceIds=excludedPlaceIds==null?List.of():List.copyOf(excludedPlaceIds); }
    public TourismPlaceFeedRequest(String region,String category,int limit,String seed) { this(region,category,limit,seed,List.of()); }
}

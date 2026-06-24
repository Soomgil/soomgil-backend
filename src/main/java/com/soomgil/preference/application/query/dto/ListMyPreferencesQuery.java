package com.soomgil.preference.application.query.dto;

import com.soomgil.common.cqrs.Query;
import com.soomgil.preference.api.dto.MyPreferenceSummary;

/**
 * 현재 사용자의 여행 취향 분석 결과를 조회하는 query.
 */
public record ListMyPreferencesQuery() implements Query<MyPreferenceSummary> {
}

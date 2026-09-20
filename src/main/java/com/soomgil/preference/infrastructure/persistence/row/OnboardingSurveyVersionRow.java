package com.soomgil.preference.infrastructure.persistence.row;

import java.util.UUID;

/**
 * 활성 가입 취향 설문 version의 저장소 읽기 모델.
 */
public record OnboardingSurveyVersionRow(UUID id, String code, int requiredPlaceCount) {
}

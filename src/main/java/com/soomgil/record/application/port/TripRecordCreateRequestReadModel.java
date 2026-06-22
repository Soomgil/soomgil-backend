package com.soomgil.record.application.port;

import java.util.UUID;

/** 여행 기록 생성 idempotency 저장 결과. */
public record TripRecordCreateRequestReadModel(String requestHash, UUID recordId) {
}

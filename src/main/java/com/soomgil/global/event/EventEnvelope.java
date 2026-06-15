package com.soomgil.global.event;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 모듈 간 공유되는 도메인 이벤트의 공통 envelope.
 *
 * <p>{@code payload}는 이벤트별 record로 강하게 타입을 정하고, envelope은 추적과 routing에 필요한
 * 공통 metadata를 담는다. {@code actorUserId}는 시스템 작업처럼 사용자가 없는 이벤트에서는 {@code null}일 수 있다.
 *
 * @param <T> 이벤트 payload 타입
 */
public record EventEnvelope<T>(
	UUID eventId,
	String eventType,
	int schemaVersion,
	Instant occurredAt,
	String aggregateType,
	String aggregateId,
	UUID actorUserId,
	T payload
) {

	public EventEnvelope {
		Objects.requireNonNull(eventId, "eventId must not be null");
		Objects.requireNonNull(occurredAt, "occurredAt must not be null");
		Objects.requireNonNull(payload, "payload must not be null");
		eventType = requireNotBlank(eventType, "eventType");
		aggregateType = requireNotBlank(aggregateType, "aggregateType");
		aggregateId = requireNotBlank(aggregateId, "aggregateId");
		if (schemaVersion < 1) {
			throw new IllegalArgumentException("schemaVersion must be greater than or equal to 1");
		}
	}

	/**
	 * schema version 1의 새 event envelope을 생성한다.
	 *
	 * @param eventType 이벤트 종류. 예: {@code trip.created}
	 * @param aggregateType 이벤트 대상 종류. 예: {@code trip}
	 * @param aggregateId 이벤트 대상 ID
	 * @param actorUserId 이벤트를 일으킨 사용자 ID, 시스템 이벤트면 {@code null}
	 * @param payload 이벤트별 payload
	 * @param clock 발생 시각을 만들 clock
	 * @param <T> payload 타입
	 * @return 생성된 event envelope
	 */
	public static <T> EventEnvelope<T> create(
		String eventType,
		String aggregateType,
		String aggregateId,
		UUID actorUserId,
		T payload,
		Clock clock
	) {
		return new EventEnvelope<>(
			UUID.randomUUID(),
			eventType,
			1,
			Instant.now(clock),
			aggregateType,
			aggregateId,
			actorUserId,
			payload
		);
	}

	private static String requireNotBlank(String value, String name) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(name + " must not be blank");
		}
		return value;
	}
}

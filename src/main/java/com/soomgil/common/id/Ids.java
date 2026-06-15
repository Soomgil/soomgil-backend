package com.soomgil.common.id;

import java.util.UUID;

/**
 * UUID 기반 식별자 생성과 파싱을 위한 공통 helper.
 *
 * <p>도메인에서 외부 입력 문자열을 UUID로 바꿀 때 field 이름을 함께 넘겨야 한다.
 * 그래야 실패 메시지와 테스트에서 어떤 입력이 잘못됐는지 추적할 수 있다.
 */
public final class Ids {

	private Ids() {
	}

	/**
	 * 새 UUID 식별자를 생성한다.
	 *
	 * @return 새 UUID
	 */
	public static UUID newUuid() {
		return UUID.randomUUID();
	}

	/**
	 * 문자열을 UUID로 변환한다.
	 *
	 * @param value UUID 문자열
	 * @param fieldName 실패 메시지에 포함할 입력 field 이름
	 * @return 파싱된 UUID
	 */
	public static UUID parseUuid(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}

		try {
			return UUID.fromString(value);
		}
		catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(fieldName + " must be a valid UUID", exception);
		}
	}
}

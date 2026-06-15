package com.soomgil.common.validation;

/**
 * 도메인과 application 계층에서 반복되는 기본 검증 규칙.
 *
 * <p>이 helper는 Spring Bean Validation을 대체하지 않는다. API DTO의 입력 검증은 Bean Validation으로 처리하고,
 * domain model/policy 내부에서 마지막으로 보장해야 하는 불변 조건을 표현할 때 사용한다.
 */
public final class ValidationRules {

	private ValidationRules() {
	}

	/**
	 * 문자열이 null이 아니고 공백만으로 구성되지 않았는지 확인한다.
	 *
	 * @param value 검증할 문자열
	 * @param fieldName 실패 메시지에 포함할 field 이름
	 * @return 원본 문자열
	 */
	public static String requireNotBlank(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}

	/**
	 * 값이 1 이상인지 확인한다.
	 *
	 * @param value 검증할 값
	 * @param fieldName 실패 메시지에 포함할 field 이름
	 * @return 원본 값
	 */
	public static long requirePositive(long value, String fieldName) {
		if (value < 1) {
			throw new IllegalArgumentException(fieldName + " must be greater than 0");
		}
		return value;
	}

	/**
	 * 값이 0 이상인지 확인한다.
	 *
	 * @param value 검증할 값
	 * @param fieldName 실패 메시지에 포함할 field 이름
	 * @return 원본 값
	 */
	public static long requireNonNegative(long value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must be greater than or equal to 0");
		}
		return value;
	}

	/**
	 * 값이 inclusive 범위 안에 있는지 확인한다.
	 *
	 * @param value 검증할 값
	 * @param minInclusive 최소 허용값
	 * @param maxInclusive 최대 허용값
	 * @param fieldName 실패 메시지에 포함할 field 이름
	 * @return 원본 값
	 */
	public static long requireBetween(long value, long minInclusive, long maxInclusive, String fieldName) {
		if (minInclusive > maxInclusive) {
			throw new IllegalArgumentException("minInclusive must be less than or equal to maxInclusive");
		}
		if (value < minInclusive || value > maxInclusive) {
			throw new IllegalArgumentException(
				fieldName + " must be between " + minInclusive + " and " + maxInclusive
			);
		}
		return value;
	}
}

package com.soomgil.trip.domain.model;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

/**
 * 여행방 제목과 대표 목적지 문자열 정책.
 *
 * <p>API validation이 누락되거나 다른 application entrypoint가 생겨도 domain 계층에서
 * 빈 제목과 최대 길이 초과를 마지막으로 차단한다.
 */
public final class TripTitlePolicy {

	private static final int MAX_LENGTH = 160;

	private TripTitlePolicy() {
	}

	/**
	 * 여행방 제목을 trim하고 유효성을 검증한다.
	 *
	 * @param title 사용자 입력 제목
	 * @return 저장 가능한 제목
	 */
	public static String normalizeTitle(String title) {
		String normalized = normalizeOptionalText(title);
		if (normalized == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Trip title must not be blank.");
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Trip title must be 160 characters or less.");
		}
		return normalized;
	}

	/**
	 * 선택 문자열을 trim하고 빈 문자열은 null로 바꾼다.
	 *
	 * @param value 사용자 입력 문자열
	 * @return null 또는 trim된 문자열
	 */
	public static String normalizeOptionalText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		if (normalized.isEmpty()) {
			return null;
		}
		if (normalized.length() > MAX_LENGTH) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Trip text field must be 160 characters or less.");
		}
		return normalized;
	}
}

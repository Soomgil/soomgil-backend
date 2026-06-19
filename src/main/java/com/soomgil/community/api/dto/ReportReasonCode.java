package com.soomgil.community.api.dto;

/**
 * 신고 사유 코드.
 *
 * <p>SPAM은 스팸/광고, INAPPROPRIATE는 부적절한 콘텐츠, HARASSMENT_OR_HATE는 괴롭힘/혐오 표현,
 * RIGHTS_VIOLATION은 저작권 등 권리 침해, OTHER는 기타 사유를 나타낸다.
 */
public enum ReportReasonCode {
	SPAM,
	INAPPROPRIATE,
	HARASSMENT_OR_HATE,
	RIGHTS_VIOLATION,
	OTHER
}

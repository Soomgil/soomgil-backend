package com.soomgil.ai.application;

/**
 * 일정 기반 체크리스트 요청의 저장 범위를 판별한다.
 *
 * <p>일차별 요청은 전체 여행 표현을 함께 포함하더라도 DAY 범위를 우선한다.
 */
public final class AiChecklistRequestScope {

	private AiChecklistRequestScope() {
	}

	/** 전체 여행의 공통 준비물 요청이면 true를 반환한다. */
	public static boolean isTripWide(String question) {
		String normalized = question == null ? "" : question.replaceAll("[\\s!?.,~]+", "");
		if (normalized.matches(".*(일차별|각일차|각각|\\d+일차).*")) return false;
		return normalized.matches(".*(전체|공통|여행방|여행계획|일정보고).*")
			&& normalized.matches(".*(준비물|자동|생성|만들).*");
	}
}

package com.soomgil.preference.domain.policy;

/**
 * 태그 통계를 만든 반응 데이터 source.
 *
 * <p>하나의 run은 하나의 source만 사용하며 서로 다른 source의 반응을 합산하지 않는다.
 */
public enum TagStatisticSource {
	AI_ONLY_DEFAULT,
	SYNTHETIC_PERSONA,
	REAL_USER
}

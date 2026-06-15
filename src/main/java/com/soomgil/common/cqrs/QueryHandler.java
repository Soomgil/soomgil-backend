package com.soomgil.common.cqrs;

/**
 * 하나의 query를 처리하는 읽기 use case 계약.
 *
 * <p>handler는 read-only 조회, projection 조립, page 응답 변환을 담당한다.
 * 쓰기 side effect가 필요하면 query가 아니라 command로 분리한다.
 *
 * @param <Q> 처리할 query 타입
 * @param <R> query 조회 결과 타입
 */
@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

	/**
	 * query를 처리하고 조회 결과를 반환한다.
	 *
	 * @param query 처리할 query
	 * @return query 조회 결과
	 */
	R handle(Q query);
}

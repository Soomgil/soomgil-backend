package com.soomgil.common.cqrs;

/**
 * 상태를 변경하지 않는 application 읽기 요청을 나타내는 marker interface.
 *
 * <p>구현 record는 조회 조건을 담고, {@code R}은 조회 결과 view 또는 page response 타입이다.
 * Query는 읽기 의도만 표현하며, 실행 책임은 {@link QueryHandler}에 둔다.
 *
 * @param <R> query 조회 결과 타입
 */
public interface Query<R> {
}

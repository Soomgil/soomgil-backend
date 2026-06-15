package com.soomgil.common.cqrs;

/**
 * 상태를 변경하는 application 요청을 나타내는 marker interface.
 *
 * <p>구현 record는 하나의 사용자 의도를 담고, {@code R}은 해당 command를 처리한 뒤 반환되는
 * application result 타입이다. Command 자체는 실행되지 않으며, 실행 책임은 {@link CommandHandler}에 둔다.
 *
 * @param <R> command 처리 결과 타입
 */
public interface Command<R> {
}

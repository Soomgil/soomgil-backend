package com.soomgil.common.cqrs;

/**
 * 하나의 command를 처리하는 쓰기 use case 계약.
 *
 * <p>handler는 command 하나에 집중하며 transaction, 권한 확인, domain 정책 적용, persistence 호출을 조율한다.
 * 결과가 없는 command도 {@code null}이나 {@code Void} 대신 {@link NoResult}를 반환한다.
 *
 * @param <C> 처리할 command 타입
 * @param <R> command 처리 결과 타입
 */
@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {

	/**
	 * command를 처리하고 명시적인 result를 반환한다.
	 *
	 * @param command 처리할 command
	 * @return command 처리 결과
	 */
	R handle(C command);
}

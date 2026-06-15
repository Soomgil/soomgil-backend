package com.soomgil.common.cqrs;

@FunctionalInterface
public interface QueryHandler<Q extends Query<R>, R> {

	R handle(Q query);
}

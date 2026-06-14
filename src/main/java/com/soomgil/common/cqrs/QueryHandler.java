package com.soomgil.common.cqrs;

@FunctionalInterface
public interface QueryHandler<R, Q extends Query<R>> {

	R handle(Q query);
}

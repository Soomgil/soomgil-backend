package com.soomgil.common.cqrs;

@FunctionalInterface
public interface CommandHandler<R, C extends Command<R>> {

	R handle(C command);
}

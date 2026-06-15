package com.soomgil.common.cqrs;

@FunctionalInterface
public interface CommandHandler<C extends Command<R>, R> {

	R handle(C command);
}

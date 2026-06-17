package com.soomgil.collaboration.application.port;

/**
 * 현재 협업 요청의 WebSocket session ID를 제공한다.
 */
@FunctionalInterface
public interface CollaborationSessionIdProvider {

	String currentSessionId();
}

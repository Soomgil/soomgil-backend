package com.soomgil.collaboration.application.port;

/**
 * 저장된 협업 command event를 실시간 채널로 발행하는 port.
 */
public interface CollaborationEventBroadcaster {

	/**
	 * 협업 command event를 발행한다.
	 *
	 * @param commandEventId 저장된 command event ID
	 * @param event 저장된 command event
	 */
	void broadcast(Long commandEventId, CollaborationCommandEvent event);
}

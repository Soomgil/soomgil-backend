package com.soomgil.collaboration.application.port;

/**
 * 협업 command event 쓰기 persistence 계약.
 */
public interface CollaborationCommandEventRepository {

	/**
	 * 협업 write command event를 저장한다.
	 *
	 * @param event 저장할 event
	 */
	void save(CollaborationCommandEvent event);
}

package com.soomgil.collaboration.infrastructure.persistence.mapper;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 협업 command event SQL mapper.
 */
@Mapper
public interface CollaborationCommandEventMapper {

	/**
	 * 협업 command event row를 추가한다.
	 *
	 * @param event 저장할 event
	 */
	void insertEvent(@Param("event") CollaborationCommandEvent event);
}

package com.soomgil.collaboration.infrastructure.persistence.repository;

import com.soomgil.collaboration.application.port.CollaborationCommandEvent;
import com.soomgil.collaboration.application.port.CollaborationCommandEventRepository;
import com.soomgil.collaboration.infrastructure.persistence.mapper.CollaborationCommandEventMapper;
import java.util.Objects;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 협업 command event repository.
 */
@Repository
public class MyBatisCollaborationCommandEventRepository implements CollaborationCommandEventRepository {

	private final CollaborationCommandEventMapper mapper;

	public MyBatisCollaborationCommandEventRepository(CollaborationCommandEventMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public void save(CollaborationCommandEvent event) {
		mapper.insertEvent(event);
	}
}

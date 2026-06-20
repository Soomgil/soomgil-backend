package com.soomgil.record.infrastructure.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soomgil.record.infrastructure.persistence.mapper.RecordMediaAccessMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MyBatisRecordMediaAccessRepositoryTest {

	private static final UUID RECORD_ID = UUID.fromString("30000000-0000-0000-0000-000000000001");
	private static final UUID USER_ID = UUID.fromString("20000000-0000-0000-0000-000000000001");
	private static final UUID MEDIA_ID = UUID.fromString("40000000-0000-0000-0000-000000000001");

	private final RecordMediaAccessMapper mapper = mock(RecordMediaAccessMapper.class);
	private final MyBatisRecordMediaAccessRepository repository = new MyBatisRecordMediaAccessRepository(mapper);

	@Test
	void requiresEveryDistinctMediaFileToBeLinkable() {
		when(mapper.countLinkable(RECORD_ID, USER_ID, List.of(MEDIA_ID))).thenReturn(1L);

		boolean result = repository.areLinkable(RECORD_ID, USER_ID, List.of(MEDIA_ID, MEDIA_ID));

		assertThat(result).isTrue();
		verify(mapper).countLinkable(RECORD_ID, USER_ID, List.of(MEDIA_ID));
	}

	@Test
	void rejectsWhenAnyMediaFileIsNotLinkable() {
		UUID otherMediaId = UUID.fromString("40000000-0000-0000-0000-000000000002");
		when(mapper.countLinkable(RECORD_ID, USER_ID, List.of(MEDIA_ID, otherMediaId))).thenReturn(1L);

		assertThat(repository.areLinkable(RECORD_ID, USER_ID, List.of(MEDIA_ID, otherMediaId))).isFalse();
	}
}

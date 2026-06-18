package com.soomgil.global.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.UUID;
import org.apache.ibatis.type.JdbcType;
import org.junit.jupiter.api.Test;

class UuidTypeHandlerTest {

	private final UuidTypeHandler handler = new UuidTypeHandler();

	@Test
	void writesUuidAsPostgresOtherType() throws Exception {
		PreparedStatement statement = mock(PreparedStatement.class);
		UUID value = UUID.fromString("10000000-0000-0000-0000-000000000001");

		handler.setNonNullParameter(statement, 1, value, JdbcType.OTHER);

		verify(statement).setObject(1, value, Types.OTHER);
	}

	@Test
	void readsUuidWithoutStringConversion() throws Exception {
		ResultSet resultSet = mock(ResultSet.class);
		UUID value = UUID.fromString("10000000-0000-0000-0000-000000000001");
		when(resultSet.getObject("id", UUID.class)).thenReturn(value);

		assertThat(handler.getNullableResult(resultSet, "id")).isEqualTo(value);
	}
}

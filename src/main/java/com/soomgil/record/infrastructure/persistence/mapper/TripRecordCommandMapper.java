package com.soomgil.record.infrastructure.persistence.mapper;

import com.soomgil.record.application.port.TripRecordEntryCreate;
import com.soomgil.record.application.port.TripRecordEntryUpdate;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 여행 기록 쓰기 SQL mapper.
 */
@Mapper
public interface TripRecordCommandMapper {

	void insertEntry(@Param("entry") TripRecordEntryCreate entry);

	void updateEntry(@Param("entry") TripRecordEntryUpdate entry);

	int softDeleteEntry(
		@Param("tripId") UUID tripId,
		@Param("recordId") UUID recordId,
		@Param("deletedAt") OffsetDateTime deletedAt
	);

	void deleteMediaLinks(@Param("recordId") UUID recordId);

	void insertMediaLink(
		@Param("recordId") UUID recordId,
		@Param("mediaFileId") UUID mediaFileId,
		@Param("sortOrder") int sortOrder,
		@Param("createdAt") OffsetDateTime createdAt
	);
}

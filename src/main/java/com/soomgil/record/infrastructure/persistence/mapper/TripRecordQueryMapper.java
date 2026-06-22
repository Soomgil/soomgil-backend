package com.soomgil.record.infrastructure.persistence.mapper;

import com.soomgil.record.application.port.TripRecordEntryReadModel;
import com.soomgil.record.application.port.TripRecordMediaReadModel;
import com.soomgil.record.application.port.TripRecordPhotoReadModel;
import com.soomgil.record.application.port.TripRecordPhotoSummaryReadModel;
import com.soomgil.record.application.port.TripRecordPhotoUrlReadModel;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 여행 기록 읽기 SQL mapper.
 */
@Mapper
public interface TripRecordQueryMapper {

	List<TripRecordEntryReadModel> findEntries(@Param("tripId") UUID tripId, @Param("limit") int limit, @Param("offset") int offset);

	long countEntries(@Param("tripId") UUID tripId);

	TripRecordEntryReadModel findEntry(@Param("tripId") UUID tripId, @Param("recordId") UUID recordId);

	List<TripRecordMediaReadModel> findMedia(@Param("recordId") UUID recordId);

	List<TripRecordPhotoReadModel> findPhotos(@Param("tripId") UUID tripId, @Param("limit") int limit, @Param("offset") int offset);

	long countPhotos(@Param("tripId") UUID tripId);

	List<TripRecordPhotoReadModel> findPhotosByUser(
		@Param("userId") UUID userId,
		@Param("limit") int limit,
		@Param("offset") int offset
	);

	long countPhotosByUser(@Param("userId") UUID userId);

	TripRecordPhotoUrlReadModel findAccessiblePhotoUrl(
		@Param("userId") UUID userId,
		@Param("mediaFileId") UUID mediaFileId
	);

	List<TripRecordPhotoSummaryReadModel> findPhotoSummariesByUser(
		@Param("userId") UUID userId,
		@Param("tripIds") List<UUID> tripIds
	);
}

package com.soomgil.record.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.record.api.dto.CreateTripRecordRequest;
import com.soomgil.record.api.dto.PagedTripRecordEntry;
import com.soomgil.record.api.dto.PagedTripRecordPhoto;
import com.soomgil.record.api.dto.RecordVisibility;
import com.soomgil.record.api.dto.TripRecordEntry;
import com.soomgil.record.api.dto.TripRecordPhoto;
import com.soomgil.record.api.dto.UpdateTripRecordRequest;
import com.soomgil.record.application.port.TripRecordCommandRepository;
import com.soomgil.record.application.port.TripRecordEntryCreate;
import com.soomgil.record.application.port.TripRecordEntryReadModel;
import com.soomgil.record.application.port.TripRecordEntryUpdate;
import com.soomgil.record.application.port.TripRecordMediaReadModel;
import com.soomgil.record.application.port.TripRecordPage;
import com.soomgil.record.application.port.TripRecordPhotoPage;
import com.soomgil.record.application.port.TripRecordPhotoReadModel;
import com.soomgil.record.application.port.TripRecordQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여행 기록 entry와 media link use case를 처리한다.
 */
@Component
public class TripRecordService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;

	private final TripRecordCommandRepository commandRepository;
	private final TripRecordQueryRepository queryRepository;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public TripRecordService(
		TripRecordCommandRepository commandRepository,
		TripRecordQueryRepository queryRepository,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
		this.tripAccessGuard = Objects.requireNonNull(tripAccessGuard, "tripAccessGuard must not be null");
		this.timeProvider = Objects.requireNonNull(timeProvider, "timeProvider must not be null");
	}

	@Transactional(readOnly = true)
	public PagedTripRecordEntry listRecords(UUID tripId, UUID userId, int page, int size, List<String> sort) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		int normalizedSize = normalizeSize(size);
		int normalizedPage = normalizePage(page);
		TripRecordPage result = queryRepository.findEntries(tripId, normalizedPage, normalizedSize);
		return new PagedTripRecordEntry(
			result.items().stream().map(this::toEntry).toList(),
			pageMeta(normalizedPage, normalizedSize, result.totalElements(), sort)
		);
	}

	@Transactional
	public TripRecordEntry createRecord(UUID tripId, UUID userId, CreateTripRecordRequest request) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		validate(request.title(), request.lat(), request.lng());
		UUID recordId = Ids.newUuid();
		OffsetDateTime now = now();
		commandRepository.insertEntry(new TripRecordEntryCreate(
			recordId,
			tripId,
			request.itineraryDayId(),
			request.itineraryItemId(),
			userId,
			normalizeText(request.title()),
			normalizeText(request.caption()),
			normalizeText(request.locationName()),
			request.lat(),
			request.lng(),
			request.takenAt(),
			now,
			now
		));
		replaceMedia(recordId, request.mediaFileIds(), now);
		return getRecord(tripId, userId, recordId);
	}

	@Transactional(readOnly = true)
	public TripRecordEntry getRecord(UUID tripId, UUID userId, UUID recordId) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		return toEntry(findEntry(tripId, recordId));
	}

	@Transactional
	public TripRecordEntry updateRecord(UUID tripId, UUID userId, UUID recordId, UpdateTripRecordRequest request) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		TripRecordEntryReadModel current = findEntry(tripId, recordId);
		requireUploader(current, userId);
		validate(request.title(), request.lat(), request.lng());
		OffsetDateTime now = now();
		commandRepository.updateEntry(new TripRecordEntryUpdate(
			tripId,
			recordId,
			request.itineraryDayId(),
			request.itineraryItemId(),
			normalizeText(request.title()),
			normalizeText(request.caption()),
			normalizeText(request.locationName()),
			request.lat(),
			request.lng(),
			request.takenAt(),
			now
		));
		if (request.mediaFileIds() != null) {
			replaceMedia(recordId, request.mediaFileIds(), now);
		}
		return getRecord(tripId, userId, recordId);
	}

	@Transactional
	public void deleteRecord(UUID tripId, UUID userId, UUID recordId) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		TripRecordEntryReadModel current = findEntry(tripId, recordId);
		requireUploader(current, userId);
		if (!commandRepository.softDeleteEntry(tripId, recordId, now())) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip record was not found.");
		}
		commandRepository.deleteMediaLinks(recordId);
	}

	@Transactional(readOnly = true)
	public PagedTripRecordPhoto listPhotos(UUID tripId, UUID userId, int page, int size, List<String> sort) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		int normalizedSize = normalizeSize(size);
		int normalizedPage = normalizePage(page);
		TripRecordPhotoPage result = queryRepository.findPhotos(tripId, normalizedPage, normalizedSize);
		return new PagedTripRecordPhoto(
			result.items().stream().map(this::toPhoto).toList(),
			pageMeta(normalizedPage, normalizedSize, result.totalElements(), sort)
		);
	}

	@Transactional(readOnly = true)
	public PagedTripRecordPhoto listPhotos(UUID userId, int page, int size, List<String> sort) {
		int normalizedSize = normalizeSize(size);
		int normalizedPage = normalizePage(page);
		TripRecordPhotoPage result = queryRepository.findPhotosByUser(userId, normalizedPage, normalizedSize);
		return new PagedTripRecordPhoto(
			result.items().stream().map(this::toPhoto).toList(),
			pageMeta(normalizedPage, normalizedSize, result.totalElements(), sort)
		);
	}

	private TripRecordEntryReadModel findEntry(UUID tripId, UUID recordId) {
		return queryRepository.findEntry(tripId, recordId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip record was not found."));
	}

	private void replaceMedia(UUID recordId, List<UUID> mediaFileIds, OffsetDateTime now) {
		commandRepository.deleteMediaLinks(recordId);
		if (mediaFileIds == null || mediaFileIds.isEmpty()) {
			return;
		}
		commandRepository.insertMediaLinks(recordId, mediaFileIds.stream().distinct().toList(), now);
	}

	private TripRecordEntry toEntry(TripRecordEntryReadModel entry) {
		List<MediaFile> media = queryRepository.findMedia(entry.id()).stream().map(this::toMediaFile).toList();
		return new TripRecordEntry(
			entry.id(),
			entry.tripId(),
			entry.itineraryDayId(),
			entry.itineraryItemId(),
			userSummary(entry.uploadedByUserId()),
			entry.title(),
			entry.caption(),
			entry.locationName(),
			entry.lat(),
			entry.lng(),
			entry.takenAt(),
			RecordVisibility.valueOf(entry.visibility()),
			entry.status(),
			media,
			entry.createdAt()
		);
	}

	private TripRecordPhoto toPhoto(TripRecordPhotoReadModel photo) {
		return new TripRecordPhoto(
			photo.tripId(),
			photo.tripTitle(),
			photo.recordId(),
			photo.itineraryDayId(),
			photo.itineraryItemId(),
			toMediaFile(photo),
			userSummary(photo.uploadedByUserId()),
			photo.takenAt(),
			photo.createdAt()
		);
	}

	private MediaFile toMediaFile(TripRecordMediaReadModel media) {
		return new MediaFile(
			media.mediaFileId(),
			media.publicUrl(),
			media.mimeType(),
			media.byteSize(),
			media.width(),
			media.height(),
			media.status(),
			media.createdAt()
		);
	}

	private MediaFile toMediaFile(TripRecordPhotoReadModel photo) {
		return new MediaFile(
			photo.mediaFileId(),
			photo.publicUrl(),
			photo.mimeType(),
			photo.byteSize(),
			photo.width(),
			photo.height(),
			photo.mediaStatus(),
			photo.mediaCreatedAt()
		);
	}

	private UserSummary userSummary(UUID userId) {
		return new UserSummary(userId, "사용자", null);
	}

	private void requireUploader(TripRecordEntryReadModel entry, UUID userId) {
		if (!entry.uploadedByUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Trip record uploader access is required.");
		}
	}

	private void validate(String title, Double lat, Double lng) {
		if (title != null && title.length() > 160) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Title must be 160 characters or fewer.");
		}
		if (lat != null && (lat < -90.0 || lat > 90.0)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Latitude must be between -90 and 90.");
		}
		if (lng != null && (lng < -180.0 || lng > 180.0)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Longitude must be between -180 and 180.");
		}
	}

	private String normalizeText(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isEmpty() ? null : normalized;
	}

	private int normalizePage(int page) {
		return Math.max(page, 0);
	}

	private int normalizeSize(int size) {
		if (size <= 0) {
			return DEFAULT_SIZE;
		}
		return Math.min(size, MAX_SIZE);
	}

	private PageMeta pageMeta(int page, int size, long totalElements, List<String> sort) {
		int totalPages = totalElements == 0 ? 0 : (int) Math.ceil(totalElements / (double) size);
		return new PageMeta(page, size, totalElements, totalPages, sort == null ? List.of() : sort);
	}

	private OffsetDateTime now() {
		return OffsetDateTime.ofInstant(timeProvider.now(), ZoneOffset.UTC);
	}
}

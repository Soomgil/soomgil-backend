package com.soomgil.record.application.handler;

import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.common.id.Ids;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.global.storage.ObjectStorageGateway;
import com.soomgil.global.storage.PresignedStorageRead;
import com.soomgil.global.storage.StorageObjectKey;
import com.soomgil.global.storage.StorageReadRequest;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.record.api.dto.CreateTripRecordRequest;
import com.soomgil.record.api.dto.PagedTripRecordEntry;
import com.soomgil.record.api.dto.PagedTripRecordPhoto;
import com.soomgil.record.api.dto.RecordVisibility;
import com.soomgil.record.api.dto.TripRecordEntry;
import com.soomgil.record.api.dto.TripRecordDay;
import com.soomgil.record.api.dto.TripRecordPhoto;
import com.soomgil.record.api.dto.TripRecordPhotoReadUrl;
import com.soomgil.record.api.dto.TripRecordPhotoSummary;
import com.soomgil.record.api.dto.TripRecordPhotoSummaryResponse;
import com.soomgil.record.api.dto.UpdateTripRecordRequest;
import com.soomgil.record.application.port.ItineraryReferenceRepository;
import com.soomgil.record.application.port.RecordMediaAccessRepository;
import com.soomgil.record.application.port.TripRecordCommandRepository;
import com.soomgil.record.application.port.TripRecordCreateRequestReadModel;
import com.soomgil.record.application.port.TripRecordEntryCreate;
import com.soomgil.record.application.port.TripRecordEntryReadModel;
import com.soomgil.record.application.port.TripRecordEntryUpdate;
import com.soomgil.record.application.port.TripRecordMediaReadModel;
import com.soomgil.record.application.port.TripRecordPage;
import com.soomgil.record.application.port.TripRecordPhotoPage;
import com.soomgil.record.application.port.TripRecordPhotoReadModel;
import com.soomgil.record.application.port.TripRecordPhotoSummaryReadModel;
import com.soomgil.record.application.port.TripRecordPhotoUrlReadModel;
import com.soomgil.record.application.port.TripRecordQueryRepository;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여행 기록 entry와 media link use case를 처리한다.
 */
@Component
public class TripRecordService {

	private static final int DEFAULT_SIZE = 20;
	private static final int MAX_SIZE = 100;
	private static final Duration RECORD_MEDIA_READ_VALIDITY = Duration.ofMinutes(30);

	private final TripRecordCommandRepository commandRepository;
	private final TripRecordQueryRepository queryRepository;
	private final ItineraryReferenceRepository itineraryReferenceRepository;
	private final RecordMediaAccessRepository mediaAccessRepository;
	private final ObjectStorageGateway objectStorageGateway;
	private final TripAccessGuard tripAccessGuard;
	private final TimeProvider timeProvider;

	public TripRecordService(
		TripRecordCommandRepository commandRepository,
		TripRecordQueryRepository queryRepository,
		ItineraryReferenceRepository itineraryReferenceRepository,
		RecordMediaAccessRepository mediaAccessRepository,
		ObjectStorageGateway objectStorageGateway,
		TripAccessGuard tripAccessGuard,
		TimeProvider timeProvider
	) {
		this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
		this.queryRepository = Objects.requireNonNull(queryRepository, "queryRepository must not be null");
		this.itineraryReferenceRepository = Objects.requireNonNull(
			itineraryReferenceRepository,
			"itineraryReferenceRepository must not be null"
		);
		this.mediaAccessRepository = Objects.requireNonNull(mediaAccessRepository, "mediaAccessRepository must not be null");
		this.objectStorageGateway = Objects.requireNonNull(objectStorageGateway, "objectStorageGateway must not be null");
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
		return createRecord(tripId, userId, request, null);
	}

	@Transactional
	public TripRecordEntry createRecord(
		UUID tripId, UUID userId, CreateTripRecordRequest request, String idempotencyKey
	) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		validate(request.title(), request.lat(), request.lng());
		validateItineraryReferences(tripId, request.itineraryDayId(), request.itineraryItemId());
		String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
		String requestHash = requestHash(request);
		if (normalizedKey != null) {
			commandRepository.lockCreateRequest(userId, tripId, normalizedKey);
			TripRecordCreateRequestReadModel existing = commandRepository.findCreateRequest(userId, tripId, normalizedKey);
			if (existing != null) {
				if (!existing.requestHash().equals(requestHash)) {
					throw new BusinessException(ErrorCode.CONFLICT, "Idempotency key was reused with another request.");
				}
				return getRecord(tripId, userId, existing.recordId());
			}
		}
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
		replaceMedia(recordId, userId, request.mediaFileIds(), now);
		if (normalizedKey != null) {
			commandRepository.insertCreateRequest(userId, tripId, normalizedKey, requestHash, recordId, now);
		}
		return getRecord(tripId, userId, recordId);
	}

	private String normalizeIdempotencyKey(String value) {
		if (value == null || value.isBlank()) return null;
		String normalized = value.trim();
		if (normalized.length() < 8 || normalized.length() > 128) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Idempotency-Key must contain 8 to 128 characters.");
		}
		return normalized;
	}

	private String requestHash(CreateTripRecordRequest request) {
		String canonical = String.join("\u001f",
			Objects.toString(request.itineraryDayId(), ""), Objects.toString(request.itineraryItemId(), ""),
			Objects.toString(request.title(), ""), Objects.toString(request.caption(), ""),
			Objects.toString(request.locationName(), ""), Objects.toString(request.lat(), ""),
			Objects.toString(request.lng(), ""), Objects.toString(request.takenAt(), ""),
			Objects.toString(request.mediaFileIds(), "")
		);
		try {
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
				.digest(canonical.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is unavailable.", exception);
		}
	}

	@Transactional(readOnly = true)
	public TripRecordEntry getRecord(UUID tripId, UUID userId, UUID recordId) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		return toEntry(findEntry(tripId, recordId));
	}

	@Transactional(readOnly = true)
	public List<TripRecordDay> listDays(UUID tripId, UUID userId) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		return queryRepository.findDays(tripId).stream()
			.map(day -> new TripRecordDay(day.id(), day.dayNumber(), day.date()))
			.toList();
	}

	@Transactional
	public TripRecordEntry updateRecord(UUID tripId, UUID userId, UUID recordId, UpdateTripRecordRequest request) {
		tripAccessGuard.requireActiveMember(tripId, userId);
		TripRecordEntryReadModel current = findEntry(tripId, recordId);
		requireUploader(current, userId);
		UUID itineraryDayId = request.itineraryDayIdProvided() ? request.itineraryDayId() : current.itineraryDayId();
		UUID itineraryItemId = request.itineraryItemIdProvided() ? request.itineraryItemId() : current.itineraryItemId();
		String title = request.titleProvided() ? normalizeText(request.title()) : current.title();
		String caption = request.captionProvided() ? normalizeText(request.caption()) : current.caption();
		String locationName = request.locationNameProvided() ? normalizeText(request.locationName()) : current.locationName();
		Double lat = request.latProvided() ? request.lat() : current.lat();
		Double lng = request.lngProvided() ? request.lng() : current.lng();
		OffsetDateTime takenAt = request.takenAtProvided() ? request.takenAt() : current.takenAt();
		validate(title, lat, lng);
		validateItineraryReferences(tripId, itineraryDayId, itineraryItemId);
		OffsetDateTime now = now();
		commandRepository.updateEntry(new TripRecordEntryUpdate(
			tripId,
			recordId,
			itineraryDayId,
			itineraryItemId,
			title,
			caption,
			locationName,
			lat,
			lng,
			takenAt,
			now
		));
		if (request.mediaFileIdsProvided()) {
			replaceMedia(recordId, userId, request.mediaFileIds(), now);
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

	/**
	 * 요청한 여행들의 사진 개수와 대표 사진을 한 번의 repository 조회로 반환한다.
	 *
	 * <p>요청은 1개 이상 100개 이하의 여행 ID만 허용하며, 중복 ID는 최초 순서를 유지해 제거한다.
	 * 현재 사용자가 접근할 수 없는 여행이 하나라도 포함되면 전체 요청을 {@code FORBIDDEN}으로 거부한다.
	 */
	@Transactional(readOnly = true)
	public TripRecordPhotoSummaryResponse summarizePhotos(UUID userId, List<UUID> tripIds) {
		if (tripIds == null || tripIds.isEmpty() || tripIds.size() > MAX_SIZE || tripIds.stream().anyMatch(Objects::isNull)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Between 1 and 100 trip IDs are required.");
		}
		List<UUID> requestedIds = List.copyOf(new LinkedHashSet<>(tripIds));
		List<TripRecordPhotoSummaryReadModel> summaries = queryRepository.findPhotoSummariesByUser(userId, requestedIds);
		Map<UUID, TripRecordPhotoSummaryReadModel> summariesByTripId = summaries.stream()
			.collect(Collectors.toMap(TripRecordPhotoSummaryReadModel::tripId, Function.identity()));
		if (summariesByTripId.size() != requestedIds.size()) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Trip member access is required for every requested trip.");
		}
		return new TripRecordPhotoSummaryResponse(requestedIds.stream()
			.map(tripId -> {
				TripRecordPhotoSummaryReadModel summary = summariesByTripId.get(tripId);
				ResolvedMediaUrl cover = resolveMediaUrl(summary.coverPublicUrl(), summary.coverObjectKey());
				URI coverUrl = cover == null ? null : cover.url();
				return new TripRecordPhotoSummary(
					tripId,
					summary.photoCount(),
					summary.coverMediaFileId(),
					coverUrl,
					cover == null ? null : cover.expiresAt()
				);
			})
			.toList());
	}

	/**
	 * 현재 사용자가 접근 가능한 여행 기록 사진의 읽기 URL을 다시 발급한다.
	 */
	@Transactional(readOnly = true)
	public TripRecordPhotoReadUrl refreshPhotoReadUrl(UUID userId, UUID mediaFileId) {
		TripRecordPhotoUrlReadModel photo = queryRepository.findAccessiblePhotoUrl(userId, mediaFileId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.RESOURCE_NOT_FOUND,
				"Accessible trip record photo was not found."
			));
		ResolvedMediaUrl resolved = resolveMediaUrl(photo.publicUrl(), photo.objectKey());
		if (resolved == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip record photo URL was not found.");
		}
		return new TripRecordPhotoReadUrl(photo.mediaFileId(), resolved.url(), resolved.expiresAt());
	}

	private TripRecordEntryReadModel findEntry(UUID tripId, UUID recordId) {
		return queryRepository.findEntry(tripId, recordId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Trip record was not found."));
	}

	private void replaceMedia(UUID recordId, UUID userId, List<UUID> mediaFileIds, OffsetDateTime now) {
		if (mediaFileIds == null || mediaFileIds.isEmpty()) {
			commandRepository.deleteMediaLinks(recordId);
			return;
		}
		List<UUID> distinctIds = mediaFileIds.stream().distinct().toList();
		if (!mediaAccessRepository.areLinkable(recordId, userId, distinctIds)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Media files must be active and owned by the current user.");
		}
		commandRepository.deleteMediaLinks(recordId);
		commandRepository.insertMediaLinks(recordId, distinctIds, now);
	}

	private TripRecordEntry toEntry(TripRecordEntryReadModel entry) {
		List<MediaFile> media = queryRepository.findMedia(entry.id()).stream().map(this::toMediaFile).toList();
		return new TripRecordEntry(
			entry.id(),
			entry.tripId(),
			entry.itineraryDayId(),
			entry.itineraryItemId(),
			userSummary(
				entry.uploadedByUserId(),
				entry.uploadedByDisplayName(),
				entry.uploadedByProfileImageUrl()
			),
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
			photo.dayNumber(),
			photo.itineraryItemId(),
			toMediaFile(photo),
			userSummary(
				photo.uploadedByUserId(),
				photo.uploadedByDisplayName(),
				photo.uploadedByProfileImageUrl()
			),
			photo.takenAt(),
			photo.createdAt()
		);
	}

	private MediaFile toMediaFile(TripRecordMediaReadModel media) {
		ResolvedMediaUrl resolved = resolveMediaUrl(media.publicUrl(), media.objectKey());
		return new MediaFile(
			media.mediaFileId(),
			publicUrl(media.publicUrl()),
			servingUrl(media.publicUrl(), resolved),
			servingUrlExpiresAt(media.publicUrl(), resolved),
			media.mimeType(),
			media.byteSize(),
			media.width(),
			media.height(),
			media.status(),
			media.createdAt()
		);
	}

	private MediaFile toMediaFile(TripRecordPhotoReadModel photo) {
		ResolvedMediaUrl resolved = resolveMediaUrl(photo.publicUrl(), photo.objectKey());
		return new MediaFile(
			photo.mediaFileId(),
			publicUrl(photo.publicUrl()),
			servingUrl(photo.publicUrl(), resolved),
			servingUrlExpiresAt(photo.publicUrl(), resolved),
			photo.mimeType(),
			photo.byteSize(),
			photo.width(),
			photo.height(),
			photo.mediaStatus(),
			photo.mediaCreatedAt()
		);
	}

	private ResolvedMediaUrl resolveMediaUrl(String publicUrl, String objectKey) {
		if (publicUrl != null) {
			return new ResolvedMediaUrl(URI.create(publicUrl), null);
		}
		if (objectKey == null) {
			return null;
		}
		PresignedStorageRead read = objectStorageGateway.presignRead(new StorageReadRequest(
			new StorageObjectKey(objectKey),
			RECORD_MEDIA_READ_VALIDITY
		));
		return new ResolvedMediaUrl(read.readUrl(), read.expiresAt());
	}

	private URI publicUrl(String value) {
		return value == null ? null : URI.create(value);
	}

	private URI servingUrl(String publicUrl, ResolvedMediaUrl resolved) {
		return publicUrl == null && resolved != null ? resolved.url() : null;
	}

	private OffsetDateTime servingUrlExpiresAt(String publicUrl, ResolvedMediaUrl resolved) {
		return publicUrl == null && resolved != null ? resolved.expiresAt() : null;
	}

	private record ResolvedMediaUrl(URI url, OffsetDateTime expiresAt) {
	}

	private UserSummary userSummary(UUID userId, String displayName, String profileImageUrl) {
		return new UserSummary(
			userId,
			displayName == null || displayName.isBlank() ? "사용자" : displayName,
			profileImageUrl == null ? null : URI.create(profileImageUrl)
		);
	}

	private void requireUploader(TripRecordEntryReadModel entry, UUID userId) {
		if (!entry.uploadedByUserId().equals(userId)) {
			throw new BusinessException(ErrorCode.FORBIDDEN, "Trip record uploader access is required.");
		}
	}

	private void validateItineraryReferences(UUID tripId, UUID dayId, UUID itemId) {
		if (dayId != null && !itineraryReferenceRepository.existsDay(tripId, dayId)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary day must belong to the trip.");
		}
		if (itemId == null) {
			return;
		}
		UUID itemDayId = itineraryReferenceRepository.findItemDayId(tripId, itemId)
			.orElseThrow(() -> new BusinessException(
				ErrorCode.VALIDATION_FAILED,
				"Itinerary item must belong to the trip."
			));
		if (dayId != null && !dayId.equals(itemDayId)) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Itinerary item must belong to the selected day.");
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

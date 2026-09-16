package com.soomgil.media.application;

import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.media.infrastructure.persistence.MediaFileLookupMapper;
import com.soomgil.media.infrastructure.persistence.MediaFileRecord;
import java.net.URI;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 다른 도메인에서 활성 미디어 파일 정보를 조회할 때 사용하는 application 경계 서비스.
 */
@Component
@Transactional(readOnly = true)
public class MediaFileQueryService {

	private final MediaFileLookupMapper mediaFileMapper;

	public MediaFileQueryService(MediaFileLookupMapper mediaFileMapper) {
		this.mediaFileMapper = Objects.requireNonNull(mediaFileMapper, "mediaFileMapper must not be null");
	}

	/**
	 * 활성 미디어 파일을 조회한다.
	 *
	 * @param mediaFileId 미디어 파일 식별자
	 * @return 미디어 파일. 없거나 비활성이면 empty
	 */
	public Optional<MediaFile> findById(UUID mediaFileId) {
		if (mediaFileId == null) {
			return Optional.empty();
		}
		return mediaFileMapper.findById(mediaFileId).map(this::toDto);
	}

	/**
	 * 전달된 식별자 순서를 보존해 활성 미디어 파일을 조회한다.
	 *
	 * @param mediaFileIds 미디어 파일 식별자 목록
	 * @return 조회된 활성 미디어 목록
	 */
	public List<MediaFile> findByIds(List<UUID> mediaFileIds) {
		if (mediaFileIds == null || mediaFileIds.isEmpty()) {
			return List.of();
		}
		return mediaFileIds.stream()
			.map(this::findById)
			.flatMap(Optional::stream)
			.toList();
	}

	/**
	 * 특정 사용자가 소유한 활성 미디어인지 확인한다.
	 *
	 * <p>다른 도메인이 업로드된 미디어를 자기 리소스에 연결하기 전에 소유권을 검증하는 용도다.
	 * 미디어가 없거나 {@code ACTIVE}가 아니거나 소유자가 다르면 {@code false}를 반환하며,
	 * 호출자는 이 결과를 권한 실패로 변환해야 한다.
	 *
	 * @param mediaFileId 확인할 미디어 파일 식별자. null이면 항상 false
	 * @param ownerUserId 소유자로 기대하는 사용자 식별자. null이면 항상 false
	 * @return 해당 사용자가 소유한 활성 미디어면 true
	 */
	public boolean isOwnedActiveMedia(UUID mediaFileId, UUID ownerUserId) {
		if (mediaFileId == null || ownerUserId == null) {
			return false;
		}
		return mediaFileMapper.findById(mediaFileId)
			.map(record -> ownerUserId.equals(record.ownerUserId()))
			.orElse(false);
	}

	private MediaFile toDto(MediaFileRecord record) {
		return new MediaFile(
			record.id(),
			record.publicUrl() == null ? null : URI.create(record.publicUrl()),
			record.objectKey().contains("/trip-record/") ? URI.create("/api/v1/media/files/" + record.id() + "/content") : null,
			null,
			record.mimeType(),
			record.byteSize(),
			record.width(),
			record.height(),
			record.status(),
			OffsetDateTime.ofInstant(record.createdAt(), ZoneOffset.UTC)
		);
	}
}

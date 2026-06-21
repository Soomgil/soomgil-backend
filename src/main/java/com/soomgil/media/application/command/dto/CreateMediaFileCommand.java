package com.soomgil.media.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.media.domain.model.MediaFileMetadata;
import java.net.URI;
import java.util.UUID;

/** 업로드가 끝난 object를 검증하고 ACTIVE media metadata로 등록하는 command. */
public record CreateMediaFileCommand(
	UUID userId,
	String objectKey,
	URI requestedPublicUrl,
	String mimeType,
	long byteSize,
	Integer width,
	Integer height,
	String linkedResourceType,
	UUID linkedResourceId
) implements Command<MediaFileMetadata> {
}

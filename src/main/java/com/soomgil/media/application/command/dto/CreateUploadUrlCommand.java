package com.soomgil.media.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.media.application.command.dto.UploadUrlView;
import com.soomgil.media.domain.model.MediaPurpose;
import java.util.UUID;

/** 로그인 사용자가 object storage 직접 업로드 URL 발급을 요청하는 command. */
public record CreateUploadUrlCommand(
	UUID userId,
	String fileName,
	String mimeType,
	long byteSize,
	MediaPurpose purpose
) implements Command<UploadUrlView> {
}

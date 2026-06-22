package com.soomgil.media.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.id.Ids;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.api.dto.CreateMediaFileRequest;
import com.soomgil.media.api.dto.CreateUploadUrlRequest;
import com.soomgil.media.api.dto.MediaFile;
import com.soomgil.media.api.dto.UploadUrlResponse;
import com.soomgil.media.application.command.dto.CreateMediaFileCommand;
import com.soomgil.media.application.command.dto.CreateUploadUrlCommand;
import com.soomgil.media.application.command.dto.DeleteMediaFileCommand;
import com.soomgil.media.application.command.dto.UploadUrlView;
import com.soomgil.media.application.command.handler.CreateMediaFileCommandHandler;
import com.soomgil.media.application.command.handler.CreateUploadUrlCommandHandler;
import com.soomgil.media.application.command.handler.DeleteMediaFileCommandHandler;
import com.soomgil.media.domain.model.MediaFileMetadata;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/media")
public class MediaController extends ApiControllerSupport {

	private final CreateUploadUrlCommandHandler uploadUrlHandler;
	private final CreateMediaFileCommandHandler createMediaFileHandler;
	private final DeleteMediaFileCommandHandler deleteMediaFileHandler;

	public MediaController(
		CreateUploadUrlCommandHandler uploadUrlHandler,
		CreateMediaFileCommandHandler createMediaFileHandler,
		DeleteMediaFileCommandHandler deleteMediaFileHandler
	) {
		this.uploadUrlHandler = uploadUrlHandler;
		this.createMediaFileHandler = createMediaFileHandler;
		this.deleteMediaFileHandler = deleteMediaFileHandler;
	}

	@PostMapping("/upload-urls")
	@ResponseStatus(HttpStatus.CREATED)
	public UploadUrlResponse createUploadUrl(
		@Valid @RequestBody CreateUploadUrlRequest request,
		Principal principal
	) {
		UploadUrlView result = uploadUrlHandler.handle(new CreateUploadUrlCommand(
			currentUserId(principal), request.fileName(), request.mimeType(), request.byteSize(), request.purpose()
		));
		return new UploadUrlResponse(
			result.uploadUrl(), result.method(), result.objectKey(), result.headers(), result.expiresAt()
		);
	}

	@PostMapping("/files")
	@ResponseStatus(HttpStatus.CREATED)
	public MediaFile createMediaFile(
		@Valid @RequestBody CreateMediaFileRequest request,
		Principal principal
	) {
		MediaFileMetadata result = createMediaFileHandler.handle(new CreateMediaFileCommand(
			currentUserId(principal), request.objectKey(), request.publicUrl(), request.mimeType(), request.byteSize(),
			request.width(), request.height(), request.linkedResourceType(), request.linkedResourceId()
		));
		return toResponse(result);
	}

	@DeleteMapping("/files/{mediaId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void deleteMediaFile(@PathVariable UUID mediaId, Principal principal) {
		deleteMediaFileHandler.handle(new DeleteMediaFileCommand(currentUserId(principal), mediaId));
	}

	private MediaFile toResponse(MediaFileMetadata value) {
		return new MediaFile(
			value.id(), value.publicUrl(), null, null, value.mimeType(), value.byteSize(), value.width(), value.height(),
			value.status(), value.createdAt()
		);
	}

	private UUID currentUserId(Principal principal) {
		if (principal == null || principal.getName() == null || principal.getName().isBlank()) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
		try {
			return Ids.parseUuid(principal.getName(), "currentUserId");
		}
		catch (IllegalArgumentException exception) {
			throw new BusinessException(ErrorCode.UNAUTHORIZED);
		}
	}
}

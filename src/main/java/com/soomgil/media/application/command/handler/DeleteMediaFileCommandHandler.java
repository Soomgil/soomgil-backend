package com.soomgil.media.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.common.time.TimeProvider;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.media.application.command.dto.DeleteMediaFileCommand;
import com.soomgil.media.application.port.MediaFileRepository;
import com.soomgil.media.domain.model.MediaFileMetadata;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** media 소유권을 확인하고 soft delete한 뒤 7일 후 object purge를 예약한다. */
@Service
public class DeleteMediaFileCommandHandler implements CommandHandler<DeleteMediaFileCommand, NoResult> {

	private static final Duration PURGE_RETENTION = Duration.ofDays(7);
	private final MediaFileRepository repository;
	private final TimeProvider timeProvider;

	public DeleteMediaFileCommandHandler(MediaFileRepository repository, TimeProvider timeProvider) {
		this.repository = repository;
		this.timeProvider = timeProvider;
	}

	@Transactional
	@Override
	public NoResult handle(DeleteMediaFileCommand command) {
		MediaFileMetadata mediaFile = repository.findById(command.mediaFileId());
		if (mediaFile == null) {
			throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Media file was not found.");
		}
		if (!mediaFile.ownerUserId().equals(command.userId())) {
			throw new BusinessException(ErrorCode.MEDIA_OWNER_REQUIRED);
		}
		if (!"ACTIVE".equals(mediaFile.status())) {
			return NoResult.INSTANCE;
		}
		Instant deletedAt = timeProvider.now();
		if (!repository.markDeleted(command.mediaFileId(), deletedAt, deletedAt.plus(PURGE_RETENTION))) {
			throw new BusinessException(ErrorCode.CONFLICT, "Media file state changed during deletion.");
		}
		return NoResult.INSTANCE;
	}
}

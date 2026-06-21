package com.soomgil.media.application.command.dto;

import com.soomgil.common.cqrs.Command;
import com.soomgil.common.cqrs.NoResult;
import java.util.UUID;

/** 소유 media를 soft delete하고 물리 삭제 시각을 예약하는 command. */
public record DeleteMediaFileCommand(UUID userId, UUID mediaFileId) implements Command<NoResult> {
}

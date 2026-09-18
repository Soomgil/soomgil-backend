package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.preference.application.command.dto.RemoveSwipeReactionCommand;

/**
 * 장소 반응 취소 command를 처리하는 application 계약.
 */
public interface RemoveSwipeReactionCommandHandler
	extends CommandHandler<RemoveSwipeReactionCommand, NoResult> {
}

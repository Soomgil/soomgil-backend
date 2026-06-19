package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.preference.api.dto.SwipeReactionResponse;
import com.soomgil.preference.application.command.dto.UpsertSwipeReactionCommand;

/**
 * 장소 스와이프 최종 반응 저장 command를 처리하는 application 계약.
 */
public interface UpsertSwipeReactionCommandHandler
	extends CommandHandler<UpsertSwipeReactionCommand, SwipeReactionResponse> {
}

package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.common.cqrs.NoResult;
import com.soomgil.preference.application.command.dto.UnsavePlaceCommand;

/**
 * 저장 장소 해제 command를 처리하는 application 계약.
 */
public interface UnsavePlaceCommandHandler extends CommandHandler<UnsavePlaceCommand, NoResult> {
}

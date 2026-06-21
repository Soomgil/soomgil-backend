package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.preference.api.dto.SavedPlace;
import com.soomgil.preference.application.command.dto.SavePlaceCommand;

/**
 * 저장 장소 생성 command를 처리하는 application 계약.
 */
public interface SavePlaceCommandHandler extends CommandHandler<SavePlaceCommand, SavedPlace> {
}

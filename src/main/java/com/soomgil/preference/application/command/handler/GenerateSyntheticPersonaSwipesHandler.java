package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.preference.application.command.dto.GenerateSyntheticPersonaSwipesCommand;
import com.soomgil.preference.application.command.dto.GenerateSyntheticPersonaSwipesResult;

/**
 * cold-start 합성 스와이프 생성 command를 처리하는 application 계약.
 */
public interface GenerateSyntheticPersonaSwipesHandler extends CommandHandler<
	GenerateSyntheticPersonaSwipesCommand,
	GenerateSyntheticPersonaSwipesResult
> {
}

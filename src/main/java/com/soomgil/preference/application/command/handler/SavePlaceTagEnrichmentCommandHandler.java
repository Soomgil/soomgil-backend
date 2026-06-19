package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentCommand;
import com.soomgil.preference.application.command.dto.SavePlaceTagEnrichmentResult;

/**
 * 장소 태깅 실행 결과 저장 command를 처리하는 application 계약.
 */
public interface SavePlaceTagEnrichmentCommandHandler
	extends CommandHandler<SavePlaceTagEnrichmentCommand, SavePlaceTagEnrichmentResult> {
}

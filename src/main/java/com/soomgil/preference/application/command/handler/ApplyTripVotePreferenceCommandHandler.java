package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceCommand;
import com.soomgil.preference.application.command.dto.ApplyTripVotePreferenceResult;

/**
 * 투표 스티커 취향 반영 command를 처리하는 application 계약.
 *
 * <p>투표 모듈은 이 interface만 의존하고 preference의 mapper, row, DB에는 접근하지 않는다.
 */
public interface ApplyTripVotePreferenceCommandHandler
	extends CommandHandler<ApplyTripVotePreferenceCommand, ApplyTripVotePreferenceResult> {
}

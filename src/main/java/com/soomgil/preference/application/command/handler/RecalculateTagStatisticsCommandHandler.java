package com.soomgil.preference.application.command.handler;

import com.soomgil.common.cqrs.CommandHandler;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsCommand;
import com.soomgil.preference.application.command.dto.RecalculateTagStatisticsResult;

/**
 * 실제 사용자의 최종 장소 반응으로 serving 태그 통계를 생성하는 application 계약.
 */
public interface RecalculateTagStatisticsCommandHandler extends
	CommandHandler<RecalculateTagStatisticsCommand, RecalculateTagStatisticsResult> {
}

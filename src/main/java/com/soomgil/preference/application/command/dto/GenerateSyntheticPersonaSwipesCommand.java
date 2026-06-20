package com.soomgil.preference.application.command.dto;

import com.soomgil.common.cqrs.Command;

/**
 * 최신 성공 태깅 장소를 대상으로 cold-start 합성 스와이프 생성을 요청하는 command.
 *
 * @param placeLimit 한 번에 처리할 최대 장소 수, {@code 1..100}
 */
public record GenerateSyntheticPersonaSwipesCommand(
	int placeLimit
) implements Command<GenerateSyntheticPersonaSwipesResult> {
}

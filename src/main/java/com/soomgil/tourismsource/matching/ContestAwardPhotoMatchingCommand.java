package com.soomgil.tourismsource.matching;

import com.soomgil.common.cqrs.Command;
import java.util.UUID;

/**
 * 공모전 수상작 사진 1장의 관광지/지역 매칭 후보를 생성하는 command.
 *
 * @param photoId 수상작 사진 id
 */
public record ContestAwardPhotoMatchingCommand(
	UUID photoId
) implements Command<ContestAwardPhotoMatchingResult> {
}

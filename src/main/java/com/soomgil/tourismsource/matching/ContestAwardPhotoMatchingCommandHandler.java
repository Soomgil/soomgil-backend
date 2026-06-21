package com.soomgil.tourismsource.matching;

import com.soomgil.common.cqrs.CommandHandler;

/**
 * 공모전 수상작 사진 매칭 command를 처리하는 application 계약.
 */
public interface ContestAwardPhotoMatchingCommandHandler
	extends CommandHandler<ContestAwardPhotoMatchingCommand, ContestAwardPhotoMatchingResult> {
}

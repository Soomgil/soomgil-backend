package com.soomgil.ai.infrastructure.external;

import com.soomgil.ai.application.AiGuideModel;
import com.soomgil.ai.application.AiGuideRequest;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;

public class UnavailableAiGuideModel implements AiGuideModel {
	@Override
	public String reply(AiGuideRequest request) {
		throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE);
	}
}

package com.soomgil.ai.application;

public interface AiGuideModel {
	AiIntentDecision classify(AiGuideRequest request);

	AiGuideReply replyWithoutTools(AiGuideRequest request, AiIntentDecision decision);

	AiGuideReply replyWithReadTools(AiGuideRequest request, AiIntentDecision decision);

	AiGuideReply replyWithWriteTools(AiGuideRequest request, AiIntentDecision decision);
}

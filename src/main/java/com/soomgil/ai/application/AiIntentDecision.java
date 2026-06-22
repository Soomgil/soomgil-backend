package com.soomgil.ai.application;

/** 분류 의도와 진단 정보를 함께 보존한다. confidence는 0~1 범위다. */
public record AiIntentDecision(
	AiIntent intent,
	double confidence,
	String reason,
	String clarificationQuestion
) {
	public AiIntentDecision {
		intent = intent == null ? AiIntent.AMBIGUOUS : intent;
		confidence = Math.max(0.0, Math.min(1.0, confidence));
	}

	public AiIntentDecision force(AiIntent forcedIntent, String forcedReason) {
		return new AiIntentDecision(forcedIntent, 1.0, forcedReason, null);
	}
}

package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolCall;
import java.util.List;

public record AiGuideReply(String content, List<AiToolCall> toolCalls) {
}

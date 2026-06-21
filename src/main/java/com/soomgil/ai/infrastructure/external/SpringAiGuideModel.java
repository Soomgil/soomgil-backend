package com.soomgil.ai.infrastructure.external;

import com.soomgil.ai.application.AiGuideModel;
import com.soomgil.ai.application.AiGuideRequest;
import java.util.ArrayList;
import java.util.Collections;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnBean(ChatModel.class)
public class SpringAiGuideModel implements AiGuideModel {

	private static final String SYSTEM_PROMPT = """
		당신은 숨길 여행방의 공동 AI 가이드입니다. 한국어로 짧고 구체적으로 답하세요.
		확인되지 않은 예약, 영업시간, 가격을 사실처럼 말하지 마세요.
		다른 여행방 멤버의 원시 선호 점수나 세부 태그 가중치를 공개하지 마세요.
		현재는 안내와 후보 제안만 수행하고, 실제 일정 변경을 했다고 거짓으로 말하지 마세요.
		""";

	private final ChatClient chatClient;

	public SpringAiGuideModel(ChatModel chatModel) {
		this.chatClient = ChatClient.create(chatModel);
	}

	@Override
	public String reply(AiGuideRequest request) {
		StringBuilder context = new StringBuilder();
		if (request.sessionSummary() != null && !request.sessionSummary().isBlank()) {
			context.append("이전 대화 요약: ").append(request.sessionSummary()).append('\n');
		}
		var turns = new ArrayList<>(request.recentMessages());
		Collections.reverse(turns);
		for (var turn : turns) {
			context.append(turn.role()).append(": ").append(turn.content()).append('\n');
		}
		context.append("USER: ").append(request.question());
		return chatClient.prompt().system(SYSTEM_PROMPT).user(context.toString()).call().content();
	}
}

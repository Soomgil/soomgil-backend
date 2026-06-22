package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolCall;
import java.util.List;

/** 한 번의 2차 모델 호출에 노출되는 도구 객체가 실행 결과를 반환하는 공통 계약. */
public interface AiExecutableTools {
	List<AiToolCall> executedCalls();
}

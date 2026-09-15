package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolExecutionPolicy;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.ai.tool.annotation.Tool;

/**
 * 일차 자체를 만들거나 고치는 도구.
 *
 * <p>장소를 다루는 도구와 분리한다. "4일차 추가해줘"는 일정 항목이 아니라 일차 그룹을 만드는 요청이다.
 */
public final class AiItineraryDayTools extends AiToolSupport {

	private final AiItineraryToolService itineraryToolService;

	AiItineraryDayTools(
		AiGuideRequest request,
		AiToolAuditService auditService,
		AiItineraryToolService itineraryToolService
	) {
		super(request, auditService);
		this.itineraryToolService = itineraryToolService;
	}

	@Tool(description = "여행 일정에 새 일차를 추가한다. "
		+ "\"4일차 만들어줘\", \"하루 더 추가해줘\"처럼 일차 자체를 늘리는 요청에 사용한다. "
		+ "dayNumber를 생략하면 마지막 일차 다음 번호로 자동 추가한다. "
		+ "장소를 추가하는 요청에는 사용하지 않는다.")
	public Object createItineraryDay(CreateDayInput input) {
		long version = baseVersion(input == null ? null : input.baseVersion());
		return execute(
			"createItineraryDay",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, version,
			() -> itineraryToolService.createDay(
				tripId, userId, version,
				input == null ? null : input.dayNumber(),
				parseDate(input == null ? null : input.date()),
				input == null ? null : input.title()
			)
		);
	}

	@Tool(description = "기존 일차의 제목이나 날짜를 바꾼다. "
		+ "\"2일차 이름 바꿔줘\", \"3일차 날짜를 10월 5일로\"처럼 일차 정보를 고치는 요청에 사용한다. "
		+ "여행 맥락 JSON의 days[]에서 대상 일차의 id를 찾아 itineraryDayId로 전달하고, "
		+ "찾지 못하면 dayNumber를 전달한다. 바꾸지 않을 항목은 null로 둔다.")
	public Object updateItineraryDay(UpdateDayInput input) {
		long version = baseVersion(input == null ? null : input.baseVersion());
		return execute(
			"updateItineraryDay",
			AiToolExecutionPolicy.REVERSIBLE_WRITE,
			input, version,
			() -> itineraryToolService.updateDay(
				tripId, userId, version,
				input == null ? null : input.itineraryDayId(),
				input == null ? null : input.dayNumber(),
				parseDate(input == null ? null : input.date()),
				input == null ? null : input.title()
			)
		);
	}

	/** LLM이 날짜를 문자열로 넘기므로 형식이 어긋나면 날짜 없이 진행한다. */
	private LocalDate parseDate(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(value.trim());
		}
		catch (RuntimeException exception) {
			return null;
		}
	}

	/**
	 * @param dayNumber 만들 일차 번호. null이면 마지막 일차 다음
	 * @param date {@code yyyy-MM-dd} 형식 날짜. 모르면 null
	 * @param title 일차 제목. null이면 "{n}일차"
	 */
	public record CreateDayInput(Long baseVersion, Integer dayNumber, String date, String title) {
	}

	/**
	 * @param itineraryDayId 수정할 일차 ID. 맥락 JSON의 days[].id에서 찾는다
	 * @param dayNumber ID를 모를 때 쓰는 일차 번호
	 * @param date {@code yyyy-MM-dd} 형식 날짜. 바꾸지 않으려면 null
	 * @param title 새 제목. 바꾸지 않으려면 null
	 */
	public record UpdateDayInput(
		Long baseVersion,
		UUID itineraryDayId,
		Integer dayNumber,
		String date,
		String title
	) {
	}
}

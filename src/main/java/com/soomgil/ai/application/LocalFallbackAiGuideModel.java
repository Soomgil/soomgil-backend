package com.soomgil.ai.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 외부 모델이 비활성 또는 일시 장애여도 기본 대화와 안전한 핵심 도구를 제공한다. */
public class LocalFallbackAiGuideModel implements AiGuideModel {
	private static final Pattern DAY_PATTERN = Pattern.compile("(\\d+)일차");
	private final AiTripToolsFactory toolsFactory;
	private final ObjectMapper objectMapper;

	public LocalFallbackAiGuideModel(AiTripToolsFactory toolsFactory, ObjectMapper objectMapper) {
		this.toolsFactory = toolsFactory;
		this.objectMapper = objectMapper;
	}

	@Override
	public AiIntentDecision classify(AiGuideRequest request) {
		String q = normalize(request.question());
		AiIntent intent;
		if (q.matches("(ㅎㅇ|안녕.*|반가워.*|하이|헬로|hi|hello|고마워.*|고맙습니다|감사.*)")) {
			intent = AiIntent.GENERAL_CHAT;
		}
		else if (q.matches(".*(뭐할수있어|무엇을할수|사용법|기능알려|도와줄수).*")) {
			intent = AiIntent.HELP;
		}
		else if (q.contains("체크리스트")) {
			intent = q.matches(".*(추가|만들|작성|수정|넣어|체크).*") ? AiIntent.WRITE_CHECKLIST : AiIntent.AMBIGUOUS;
		}
		else if (q.contains("메모")) {
			intent = q.matches(".*(써|작성|기록|추가|수정|바꿔|저장).*") ? AiIntent.WRITE_NOTE : AiIntent.AMBIGUOUS;
		}
		else if (q.matches(".*(옮겨|이동|재배치|순서.*바꿔).*")) {
			intent = AiIntent.MOVE_ITINERARY_ITEM;
		}
		else if (q.matches(".*(일정|일차).*(추가|넣어|등록).*|.*(추가|넣어|등록).*(일정|일차).*")) {
			intent = AiIntent.ADD_PLACE_TO_ITINERARY;
		}
		else if (q.matches(".*(추천|어디갈|어디가좋|갈만한).*")) {
			intent = AiIntent.RECOMMEND_PLACES;
		}
		else if (q.matches(".*(찾아|검색|어디있|장소알려).*")) {
			intent = AiIntent.SEARCH_PLACES;
		}
		else if (q.matches(".*(일정|일차|동선|경로).*(보여|조회|알려|확인|어떻게|뭐야).*|.*(보여|조회|알려|확인).*(일정|일차|동선|경로).*")) {
			intent = AiIntent.READ_ITINERARY;
		}
		else {
			intent = AiIntent.GENERAL_CHAT;
		}
		return new AiIntentDecision(intent, 1.0, "로컬 안전 분류", null);
	}

	@Override
	public AiGuideReply replyWithoutTools(AiGuideRequest request, AiIntentDecision decision) {
		String answer = switch (decision.intent()) {
			case HELP -> "일정 조회, 장소 검색·추천, 공동 메모와 체크리스트 작성, 일정 장소 추가·이동을 도와드릴 수 있어요. 원하시는 작업을 구체적으로 말씀해주세요.";
			case AMBIGUOUS -> decision.clarificationQuestion() == null
				? "어떤 여행 정보를 확인하거나 변경하고 싶은지 조금 더 구체적으로 알려주시겠어요?"
				: decision.clarificationQuestion();
			case UNSUPPORTED -> "그 작업은 안전상 직접 처리할 수 없어요. 일정 조회나 메모·체크리스트처럼 여행방 안에서 되돌릴 수 있는 작업은 도와드릴게요.";
			default -> greeting(request.question())
				? "안녕하세요! 여행 일정부터 장소 추천, 준비물 정리까지 무엇이든 편하게 말씀해주세요."
				: "말씀하신 내용을 확인했어요. 여행 일정이나 장소, 메모, 체크리스트와 관련해 원하는 내용을 구체적으로 알려주시면 바로 도와드릴게요.";
		};
		return new AiGuideReply(answer, List.of());
	}

	@Override
	public AiGuideReply replyWithReadTools(AiGuideRequest request, AiIntentDecision decision) {
		try {
			AiExecutableTools executable = toolsFactory.create(request, decision.intent()).getFirst();
			Object result = switch (decision.intent()) {
				case READ_ITINERARY -> ((AiItineraryReadTools) executable).getCurrentItinerary();
				case SEARCH_PLACES -> ((AiPlaceSearchTools) executable).searchPlaces(
					new AiPlaceSearchTools.SearchPlacesInput(request.question(), viewport(request), null, null)
				);
				case RECOMMEND_PLACES -> ((AiPlaceRecommendationTools) executable).recommendPlaces(
					new AiPlaceRecommendationTools.RecommendPlacesInput(viewport(request), null, null, "BASIC", 5)
				);
				default -> null;
			};
			return new AiGuideReply("조회 결과를 확인했어요. " + summary(result), executable.executedCalls());
		}
		catch (RuntimeException exception) {
			return failed("조회", exception);
		}
	}

	@Override
	public AiGuideReply replyWithWriteTools(AiGuideRequest request, AiIntentDecision decision) {
		try {
			AiExecutableTools executable = toolsFactory.create(request, decision.intent()).getFirst();
			return switch (decision.intent()) {
				case WRITE_NOTE -> writeNote(request, (AiNoteTools) executable);
				case WRITE_CHECKLIST -> writeChecklist(request, (AiChecklistTools) executable);
				case MOVE_ITINERARY_ITEM -> moveItem(request, (AiMoveItineraryItemTools) executable);
				case ADD_PLACE_TO_ITINERARY -> new AiGuideReply(
					"정확한 장소 정보가 필요해요. 먼저 장소를 검색하거나 추천받은 뒤 추가할 장소와 일차를 지정해주세요.",
					List.of()
				);
				default -> replyWithoutTools(request, decision);
			};
		}
		catch (RuntimeException exception) {
			return failed("변경", exception);
		}
	}

	private AiGuideReply writeNote(AiGuideRequest request, AiNoteTools tools) {
		String text = extractAfter(request.question(), "메모에", "메모로", "메모");
		if (text.isBlank()) return new AiGuideReply("메모에 저장할 내용을 알려주세요.", List.of());
		UUID dayId = dayId(request);
		tools.upsertNote(new AiNoteTools.ScopedTextInput(dayId == null ? "TRIP" : "DAY", dayId, text));
		return new AiGuideReply("요청한 내용을 공동 메모에 저장했어요.", tools.executedCalls());
	}

	private AiGuideReply writeChecklist(AiGuideRequest request, AiChecklistTools tools) {
		UUID dayId = dayId(request);
		AiTripContext.ChecklistSummary existing = request.tripContext() == null ? null
			: request.tripContext().checklists().stream()
				.filter(item -> dayId == null ? "TRIP".equals(item.scopeType()) : dayId.equals(item.itineraryDayId()))
				.findFirst().orElse(null);
		String itemText = extractAfter(request.question(), "체크리스트에", "체크리스트");
		if (itemText.isBlank() || normalize(request.question()).contains("만들")) {
			tools.upsertChecklist(new AiChecklistTools.ScopedTextInput(
				dayId == null ? "TRIP" : "DAY", dayId, itemText.isBlank() ? "여행 체크리스트" : itemText
			));
			return new AiGuideReply("체크리스트를 준비했어요.", tools.executedCalls());
		}
		UUID checklistId = existing == null ? null : existing.id();
		if (checklistId == null) {
			Object created = tools.upsertChecklist(new AiChecklistTools.ScopedTextInput(
				dayId == null ? "TRIP" : "DAY", dayId, "여행 체크리스트"
			));
			if (created instanceof PlanningMutationResponse mutation && mutation.checklist() != null) {
				checklistId = mutation.checklist().id();
			}
		}
		if (checklistId == null) return new AiGuideReply("체크리스트를 만들지 못했어요. 다시 시도해주세요.", tools.executedCalls());
		tools.addChecklistItem(new AiChecklistTools.ChecklistItemInput(checklistId, itemText, null));
		return new AiGuideReply("체크리스트에 ‘" + itemText + "’ 항목을 추가했어요.", tools.executedCalls());
	}

	private AiGuideReply moveItem(AiGuideRequest request, AiMoveItineraryItemTools tools) {
		if (request.tripContext() == null) return new AiGuideReply("이동할 일정 정보를 확인하지 못했어요.", List.of());
		AiTripContext.ItemSummary item = request.tripContext().days().stream().flatMap(day -> day.items().stream())
			.filter(candidate -> candidate.placeName() != null && request.question().contains(candidate.placeName()))
			.findFirst().orElse(null);
		UUID targetDayId = dayId(request);
		if (item == null || targetDayId == null) {
			return new AiGuideReply("이동할 장소 이름과 목표 일차를 함께 알려주세요. 예: ‘성심당을 2일차로 옮겨줘’.", List.of());
		}
		tools.moveItineraryItem(new AiMoveItineraryItemTools.MoveItemInput(
			request.baseVersion(), item.id(), targetDayId, null
		));
		return new AiGuideReply(item.placeName() + "을(를) 요청한 일차로 옮겼어요.", tools.executedCalls());
	}

	private UUID dayId(AiGuideRequest request) {
		if (request.tripContext() == null) return null;
		Matcher matcher = DAY_PATTERN.matcher(request.question());
		if (!matcher.find()) return null;
		int dayNumber = Integer.parseInt(matcher.group(1));
		return request.tripContext().days().stream()
			.filter(day -> day.dayNumber() != null && day.dayNumber() == dayNumber)
			.map(AiTripContext.DaySummary::id).findFirst().orElse(null);
	}

	private String extractAfter(String question, String... markers) {
		String result = question;
		for (String marker : markers) {
			int index = result.indexOf(marker);
			if (index >= 0) {
				result = result.substring(index + marker.length());
				break;
			}
		}
		return result.replaceAll("(이라고|라고)?\\s*(써줘|작성해줘|기록해줘|추가해줘|넣어줘|저장해줘|수정해줘)[.!?]?$", "")
			.trim();
	}

	private String viewport(AiGuideRequest request) {
		return request.viewport() == null ? null : request.viewport().minLng() + "," + request.viewport().minLat()
			+ "," + request.viewport().maxLng() + "," + request.viewport().maxLat();
	}

	private String summary(Object result) {
		try {
			String value = objectMapper.writeValueAsString(result);
			return value.length() > 1_200 ? value.substring(0, 1_200) + "…" : value;
		}
		catch (JsonProcessingException exception) {
			return "요청한 데이터를 불러왔습니다.";
		}
	}

	private AiGuideReply failed(String action, RuntimeException exception) {
		return new AiGuideReply(action + "을 처리하지 못했어요. 요청 내용을 확인한 뒤 다시 말씀해주세요.", List.<AiToolCall>of());
	}

	private boolean greeting(String question) {
		return normalize(question).matches("(ㅎㅇ|안녕.*|반가워.*|하이|헬로|hi|hello)");
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s!?.,~]+", "");
	}
}

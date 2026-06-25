package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiToolCall;
import com.soomgil.planning.api.dto.PlanningMutationResponse;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 외부 모델이 비활성 또는 일시 장애여도 기본 대화와 안전한 핵심 도구를 제공한다.
 *
 * <p>외부 LLM이 응답하지 못할 때 도구는 호출하지만, 자연어 요약/분석은 포기한다.
 * 그 대신 도구 결과(JSON)를 그대로 노출하지 않고, 사용자가 이해할 수 있는 안내문과
 * tool_call id를 함께 반환해 프론트가 필요시 결과를 직접 조회할 수 있게 한다.
 */
public class LocalFallbackAiGuideModel implements AiGuideModel {

	private static final Pattern DAY_PATTERN = Pattern.compile("(\\d+)일차");
	private final AiTripToolsFactory toolsFactory;

	public LocalFallbackAiGuideModel(AiTripToolsFactory toolsFactory, com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
		this.toolsFactory = toolsFactory;
	}

	@Override
	public AiIntentDecision classify(AiGuideRequest request) {
		String q = normalize(request.question());
		AiIntent intent;
		if (q.matches("(ㅎㅇ|안녕.*|반가워.*|하이|헬로|hi|hello|고마워.*|고맙습니다|감사.*)")) {
			intent = AiIntent.GENERAL_CHAT;
		}
		else if (q.matches(".*(뭐할수있어|무엇을할수|사용법|기능알려|도와줄수|뭐도와줘).*")) {
			intent = AiIntent.HELP;
		}
		else if (q.matches(".*(동선|이동경로|이동.*경로).*(최적화|정리|개선|재구성|짜줘|짜기)|"
			+ ".*(최적화|개선|재구성).*(동선|이동경로|경로)|"
			+ ".*가까운.*곳.*묶어|.*가까운.*곳.*같이|효율.*동선.*")) {
			intent = AiIntent.OPTIMIZE_ROUTE;
		}
		else if (q.matches(".*(체크리스트|준비물).*(자동|만들|생성|추천|분석|작성|알려|짜)|"
			+ ".*(자동|분석).*(체크리스트|준비물)|"
			+ ".*여행.*필요.*준비|.*예약.*필요.*체크|.*준비물.*뭐.*|.*체크리스트.*뭐.*")) {
			intent = AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY;
		}
		else if (q.matches(".*(유료|무료|장애인|유모차|접근|휴무|닫은|폐업|예약.*필수|입장료).*(빼|삭제|제거|없애)|"
			+ ".*(빼|삭제|제거|없애).*(유료|무료|장애인|유모차|접근|입장료)|"
			+ ".*장애인.*이용.*불가.*|.*유모차.*진입.*불가.*")) {
			intent = AiIntent.FILTER_PLACES_BY_CONDITION;
		}
		else if (q.matches(".*(추천|갈만한|여행지|장소).*(넣어|추가|등록|일정에).*|"
			+ ".*(넣어|추가|등록).*(추천|갈만한|여행지|장소).*")) {
			intent = AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY;
		}
		else if (q.matches(".*(삭제|지워|제거|빼줘|빼기|없애).*")) {
			intent = AiIntent.DELETE_ITINERARY_ITEM;
		}
		else if (q.matches(".*(요약|정리|분석|리뷰|코스.*봐줘|코스.*리뷰|한눈에.*보)|"
			+ ".*(여행일정|여행.*일정|전체.*일정|일정.*전체).*(어때|어떨까|봐줘|알려)")) {
			intent = AiIntent.SUMMARIZE_ITINERARY;
		}
		else if (q.contains("체크리스트")) {
			intent = q.matches(".*(추가|만들|작성|수정|넣어|체크).*") ? AiIntent.WRITE_CHECKLIST : AiIntent.AMBIGUOUS;
		}
		else if (q.contains("메모")) {
			intent = q.matches(".*(써|작성|기록|추가|수정|바꿔|저장).*") ? AiIntent.WRITE_NOTE : AiIntent.AMBIGUOUS;
		}
		else if (q.matches(".*(옮겨|이동|재배치|순서.*바꿔|2일차로|3일차로|1일차로|내일로|오늘로).*")) {
			intent = AiIntent.MOVE_ITINERARY_ITEM;
		}
		else if (q.matches(".*(일정|일차).*(추가|넣어|등록).*|.*(추가|넣어|등록).*(일정|일차).*")) {
			intent = AiIntent.ADD_PLACE_TO_ITINERARY;
		}
		else if (q.matches(".*(추천|어디갈|어디가좋|갈만한|여행지.*알려|명소).*")) {
			intent = AiIntent.RECOMMEND_PLACES;
		}
		else if (q.matches(".*(찾아|검색|어디있|장소알려).*")) {
			intent = AiIntent.SEARCH_PLACES;
		}
		else if (q.matches(".*(일정|일차|동선|경로).*(보여|조회|알려|확인|어떻게|뭐야).*|"
			+ ".*(보여|조회|알려|확인).*(일정|일차|동선|경로).*")) {
			intent = AiIntent.READ_ITINERARY;
		}
		else {
			intent = AiIntent.UNSUPPORTED;
		}
		return new AiIntentDecision(intent, 1.0, "로컬 안전 분류", null);
	}

	@Override
	public AiGuideReply replyWithoutTools(AiGuideRequest request, AiIntentDecision decision) {
		String answer = switch (decision.intent()) {
			case HELP -> "일정 조회·요약, 장소 검색·추천, 공동 메모·체크리스트 작성, 일정 장소 추가·이동, "
				+ "조건(유료/접근성) 기반 장소 삭제, 체크리스트 자동 생성, 동선 최적화까지 도와드릴 수 있어요. "
				+ "원하시는 작업을 편하게 말씀해주세요.";
			case AMBIGUOUS -> decision.clarificationQuestion() == null
				? "어떤 여행 정보를 확인하거나 변경하고 싶은지 조금 더 구체적으로 알려주시겠어요?"
				: decision.clarificationQuestion();
			case UNSUPPORTED -> "요청하신 내용에 대해 일반적인 안내는 드릴 수 있지만 "
				+ AiPlainTextFormatter.UNSUPPORTED_NOTICE;
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
				case SUMMARIZE_ITINERARY -> ((AiSummarizeItineraryTools) executable).summarizeItinerary();
				case SEARCH_PLACES -> ((AiPlaceSearchTools) executable).searchPlaces(
					new AiPlaceSearchTools.SearchPlacesInput(request.question(), viewport(request), null, null)
				);
				case RECOMMEND_PLACES -> ((AiPlaceRecommendationTools) executable).recommendPlaces(
					new AiPlaceRecommendationTools.RecommendPlacesInput(viewport(request), null, null, "BASIC", 5)
				);
				default -> null;
			};
			List<AiToolCall> calls = executable.executedCalls();
			if (calls.isEmpty()) {
				return new AiGuideReply(
					"AI 분석 서버가 일시적으로 원활하지 않아요. 잠시 후 다시 시도해주세요.", List.of()
				);
			}
			return new AiGuideReply(
				"요청한 정보를 조회했어요. 화면에서 최신 내용을 확인해주세요.",
				calls
			);
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
				case DELETE_ITINERARY_ITEM -> deleteItem(request, (AiDeleteItineraryItemTools) executable);
				case MOVE_ITINERARY_ITEM -> moveItem(request, (AiMoveItineraryItemTools) executable);
				case ADD_RECOMMENDED_PLACES_TO_ITINERARY ->
					addRecommendedPlaces(request, (AiAddRecommendedPlacesTools) executable);
				case ADD_PLACE_TO_ITINERARY -> new AiGuideReply(
					"정확한 장소 정보가 필요해요. 먼저 장소를 검색하거나 추천받은 뒤 추가할 장소와 일차를 지정해주세요.",
					List.of()
				);
				case FILTER_PLACES_BY_CONDITION -> filterPlaces(request, (AiFilterPlacesTools) executable);
				case GENERATE_CHECKLIST_FROM_ITINERARY -> generateChecklist(request, (AiGenerateChecklistTools) executable);
				case OPTIMIZE_ROUTE -> optimizeRoute(request, (AiOptimizeRouteTools) executable);
				default -> replyWithoutTools(request, decision);
			};
		}
		catch (RuntimeException exception) {
			return failed("변경", exception);
		}
	}

	private AiGuideReply addRecommendedPlaces(AiGuideRequest request, AiAddRecommendedPlacesTools tools) {
		String bbox = viewport(request);
		if (bbox == null) bbox = inferredBbox(request);
		if (bbox == null) {
			return new AiGuideReply("추천 장소를 넣으려면 지도 범위나 기존 일정 위치가 필요해요. 먼저 지도에서 지역을 잡아주세요.", List.of());
		}
		tools.addRecommendedPlacesToItinerary(new AiAddRecommendedPlacesTools.AddRecommendedPlacesInput(
			request.baseVersion(), bbox, null, null, "BASIC", requestedLimit(request.question()), dayId(request), null
		));
		return new AiGuideReply("추천 장소를 일정에 추가했어요. 이미 들어간 장소는 건너뛰었습니다.", tools.executedCalls());
	}

	private AiGuideReply filterPlaces(AiGuideRequest request, AiFilterPlacesTools tools) {
		if (request.tripContext() == null) return new AiGuideReply("삭제할 일정 정보를 확인하지 못했어요.", List.of());
		List<UUID> itemIds = request.tripContext().days().stream()
			.flatMap(day -> day.items().stream())
			.filter(item -> matchesRemovalCondition(request.question(), item))
			.map(AiTripContext.ItemSummary::id)
			.toList();
		if (itemIds.isEmpty()) {
			return new AiGuideReply("조건에 확실히 해당하는 장소를 찾지 못했어요. 삭제할 장소 이름을 직접 알려주시면 안전하게 처리할게요.", List.of());
		}
		tools.removeItineraryItemsByCondition(new AiFilterPlacesTools.RemoveItemsInput(request.baseVersion(), itemIds));
		return new AiGuideReply(itemIds.size() + "개 장소를 조건에 맞춰 일정에서 삭제했어요.", tools.executedCalls());
	}

	private AiGuideReply generateChecklist(AiGuideRequest request, AiGenerateChecklistTools tools) {
		if (requiresTripLevelChecklist(request.question())) {
			tools.generateChecklistItems(new AiGenerateChecklistTools.GenerateItemsInput(
				checklistIdForTrip(request.tripContext()), "TRIP", null, "AI 추천 준비물",
				tripChecklistItems(request), null
			));
			return new AiGuideReply("현재 여행 계획 기준으로 전체 체크리스트에 준비물을 추가했어요.", tools.executedCalls());
		}
		List<AiGenerateChecklistTools.DayChecklistInput> dayGroups = dayChecklistCandidates(request);
		if (!dayGroups.isEmpty()) {
			tools.generateChecklistItemsByDay(new AiGenerateChecklistTools.GenerateItemsByDayInput(dayGroups));
			return new AiGuideReply("현재 일정 기준으로 각 일차 체크리스트에 필요한 항목을 추가했어요.", tools.executedCalls());
		}
		tools.generateChecklistItems(new AiGenerateChecklistTools.GenerateItemsInput(
			null, "TRIP", null, "AI 추천 준비물",
			List.of("교통편 확인하기", "영업시간 확인하기", "보조배터리 챙기기"), null
		));
		return new AiGuideReply("현재 일정 기준으로 여행방 전체 체크리스트를 자동 생성했어요.", tools.executedCalls());
	}

	private AiGuideReply optimizeRoute(AiGuideRequest request, AiOptimizeRouteTools tools) {
		if (request.tripContext() == null) return new AiGuideReply("정리할 일정 정보를 확인하지 못했어요.", List.of());
		List<AiItineraryToolService.ItemMove> moves = new ArrayList<>();
		for (AiTripContext.DaySummary day : request.tripContext().days()) {
			List<AiTripContext.ItemSummary> sorted = day.items().stream()
				.sorted(Comparator
					.comparing((AiTripContext.ItemSummary item) -> item.lat() == null ? Double.MAX_VALUE : item.lat())
					.thenComparing(item -> item.lng() == null ? Double.MAX_VALUE : item.lng()))
				.toList();
			for (int index = 0; index < sorted.size(); index++) {
				AiTripContext.ItemSummary item = sorted.get(index);
				if (item.lat() == null || item.lng() == null) continue;
				if (item.sortOrder() == index) continue;
				moves.add(new AiItineraryToolService.ItemMove(
					item.id(), day.id(), index, item.placeName(), item.address(), item.lat(), item.lng(), null
				));
			}
		}
		if (moves.isEmpty()) {
			return new AiGuideReply("좌표 기준으로 조정할 만한 일정 순서 변경을 찾지 못했어요.", List.of());
		}
		tools.optimizeRoute(new AiOptimizeRouteTools.OptimizeRouteInput(request.baseVersion(), moves));
		return new AiGuideReply("좌표가 있는 장소들을 기준으로 일차별 동선을 정리했어요.", tools.executedCalls());
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
		return new AiGuideReply("체크리스트에 '" + itemText + "' 항목을 추가했어요.", tools.executedCalls());
	}

	private AiGuideReply moveItem(AiGuideRequest request, AiMoveItineraryItemTools tools) {
		if (request.tripContext() == null) return new AiGuideReply("이동할 일정 정보를 확인하지 못했어요.", List.of());
		String normalizedQuestion = normalize(request.question());
		AiTripContext.ItemSummary item = request.tripContext().days().stream()
			.flatMap(day -> day.items().stream())
			.filter(candidate -> candidate.placeName() != null
				&& normalizedQuestion.contains(normalize(candidate.placeName())))
			.findFirst().orElse(null);
		UUID targetDayId = dayId(request);
		if (item == null && targetDayId == null) {
			return new AiGuideReply(
				"어떤 장소를 어느 일차로 옮길지 알려주세요. 예: '성심당 2일차로 옮겨줘'.", List.of()
			);
		}
		if (item == null) {
			return new AiGuideReply(
				"일정에서 찾을 수 없는 장소예요. 정확한 장소 이름을 다시 알려주세요.", List.of()
			);
		}
		if (targetDayId == null) {
			return new AiGuideReply(
				item.placeName() + "을(를) 몇 일차로 옮길지 알려주세요. 예: '" + item.placeName() + " 2일차로 옮겨줘'.",
				List.of()
			);
		}
		tools.moveItineraryItem(new AiMoveItineraryItemTools.MoveItemInput(
			request.baseVersion(), item.id(), item.placeName(), targetDayId, null, null
		));
		return new AiGuideReply(item.placeName() + "을(를) 요청한 일차로 옮겼어요.", tools.executedCalls());
	}

	private AiGuideReply deleteItem(AiGuideRequest request, AiDeleteItineraryItemTools tools) {
		if (request.tripContext() == null) return new AiGuideReply("삭제할 일정 정보를 확인하지 못했어요.", List.of());
		String normalizedQuestion = normalize(request.question());
		AiTripContext.ItemSummary item = request.tripContext().days().stream()
			.flatMap(day -> day.items().stream())
			.filter(candidate -> candidate.placeName() != null
				&& normalizedQuestion.contains(normalize(candidate.placeName())))
			.findFirst().orElse(null);
		if (item == null) {
			return new AiGuideReply("일정에서 삭제할 장소 이름을 정확히 알려주세요.", List.of());
		}
		tools.deleteItineraryItem(new AiDeleteItineraryItemTools.DeleteItemInput(
			request.baseVersion(), item.id(), item.placeName()
		));
		return new AiGuideReply(item.placeName() + "을(를) 일정에서 삭제했어요.", tools.executedCalls());
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

	private String inferredBbox(AiGuideRequest request) {
		if (request.tripContext() == null) return null;
		List<AiTripContext.ItemSummary> items = request.tripContext().days().stream()
			.flatMap(day -> day.items().stream())
			.filter(item -> item.lat() != null && item.lng() != null)
			.toList();
		if (items.isEmpty()) return null;
		double minLat = items.stream().mapToDouble(AiTripContext.ItemSummary::lat).min().orElseThrow() - 0.05;
		double maxLat = items.stream().mapToDouble(AiTripContext.ItemSummary::lat).max().orElseThrow() + 0.05;
		double minLng = items.stream().mapToDouble(AiTripContext.ItemSummary::lng).min().orElseThrow() - 0.05;
		double maxLng = items.stream().mapToDouble(AiTripContext.ItemSummary::lng).max().orElseThrow() + 0.05;
		return minLng + "," + minLat + "," + maxLng + "," + maxLat;
	}

	private int requestedLimit(String question) {
		Matcher matcher = Pattern.compile("(\\d+)개").matcher(question);
		if (matcher.find()) return Math.min(10, Math.max(1, Integer.parseInt(matcher.group(1))));
		return 3;
	}

	private boolean matchesRemovalCondition(String question, AiTripContext.ItemSummary item) {
		String q = normalize(question);
		String text = normalize((item.placeName() == null ? "" : item.placeName()) + " " + (item.address() == null ? "" : item.address()));
		if (q.contains("장애인") || q.contains("휠체어") || q.contains("유모차") || q.contains("접근")) {
			if (accessibilityKnown(item)) {
				if (accessibilityUnavailable(item, "WHEELCHAIR")) return true;
				if (requiresSupportedAccessibility(q)) return !accessibilitySupports(item, "WHEELCHAIR");
				return false;
			}
			return text.matches(".*(산|등산|계단|전망대|오름|동굴|출렁다리|케이블카).*");
		}
		if (q.contains("유료") || q.contains("입장료")) {
			return text.matches(".*(월드|랜드|테마파크|박물관|미술관|전망대|케이블카|공원).*");
		}
		if (q.contains("휴무") || q.contains("닫은") || q.contains("폐업")) {
			return text.matches(".*(박물관|미술관|전시관|문화원).*");
		}
		return false;
	}

	private boolean accessibilityKnown(AiTripContext.ItemSummary item) {
		AiTripContext.AccessibilitySummary accessibility = item.accessibility();
		return accessibility != null
			&& (!accessibility.flags().isEmpty() || !accessibility.unavailableFlags().isEmpty());
	}

	private boolean accessibilitySupports(AiTripContext.ItemSummary item, String flag) {
		AiTripContext.AccessibilitySummary accessibility = item.accessibility();
		return accessibility != null && accessibility.flags().contains(flag);
	}

	private boolean accessibilityUnavailable(AiTripContext.ItemSummary item, String flag) {
		AiTripContext.AccessibilitySummary accessibility = item.accessibility();
		return accessibility != null && accessibility.unavailableFlags().contains(flag);
	}

	private boolean requiresSupportedAccessibility(String question) {
		return question.contains("가능") || question.contains("되는곳") || question.contains("되는장소")
			|| question.contains("이용가능") || question.contains("접근가능");
	}

	private List<AiGenerateChecklistTools.DayChecklistInput> dayChecklistCandidates(AiGuideRequest request) {
		if (request.tripContext() == null) return List.of();
		List<AiGenerateChecklistTools.DayChecklistInput> groups = new ArrayList<>();
		for (AiTripContext.DaySummary day : request.tripContext().days()) {
			List<String> items = new ArrayList<>();
			for (AiTripContext.ItemSummary item : day.items()) {
				String name = item.placeName() == null ? "방문지" : item.placeName();
				String text = normalize(name + " " + (item.address() == null ? "" : item.address()));
				if (text.matches(".*(월드|랜드|테마파크|케이블카|전망대).*")) items.add(name + " 예약 또는 입장권 확인하기");
				if (text.matches(".*(해수욕장|바다|수변|공원|수목원|오름|산).*")) items.add(name + " 방문 전 날씨와 편한 신발 챙기기");
				if (text.matches(".*(박물관|미술관|전시관|문화원).*")) items.add(name + " 휴무일과 운영시간 확인하기");
			}
			List<String> distinctItems = items.stream().distinct().limit(10).toList();
			if (!distinctItems.isEmpty() && day.id() != null) {
				groups.add(new AiGenerateChecklistTools.DayChecklistInput(
					checklistIdForDay(request.tripContext(), day.id()),
					day.id(),
					dayTitle(day),
					distinctItems,
					null
				));
			}
		}
		return groups;
	}

	private boolean requiresTripLevelChecklist(String question) {
		String normalized = normalize(question);
		return normalized.contains("전체") || normalized.contains("공통")
			|| normalized.contains("여행방") || normalized.contains("여행계획")
			|| normalized.contains("여행계획보고") || normalized.contains("일정보고");
	}

	private List<String> tripChecklistItems(AiGuideRequest request) {
		List<String> items = new ArrayList<>();
		items.add("교통편과 이동 시간 확인하기");
		items.add("보조배터리와 충전기 챙기기");
		items.add("날씨에 맞는 옷과 편한 신발 챙기기");
		if (request.tripContext() != null) {
			boolean hasOutdoor = request.tripContext().days().stream()
				.flatMap(day -> day.items().stream())
				.anyMatch(item -> normalize((item.placeName() == null ? "" : item.placeName())
					+ " " + (item.address() == null ? "" : item.address()))
					.matches(".*(해수욕장|바다|수변|공원|수목원|오름|산|전망대).*"));
			boolean hasTicketed = request.tripContext().days().stream()
				.flatMap(day -> day.items().stream())
				.anyMatch(item -> normalize((item.placeName() == null ? "" : item.placeName())
					+ " " + (item.address() == null ? "" : item.address()))
					.matches(".*(월드|랜드|테마파크|케이블카|전망대|박물관|미술관).*"));
			if (hasOutdoor) items.add("야외 일정용 물과 자외선 차단제 챙기기");
			if (hasTicketed) items.add("입장권 예약 여부와 운영시간 확인하기");
		}
		return items.stream().distinct().limit(10).toList();
	}

	private UUID checklistIdForDay(AiTripContext context, UUID dayId) {
		return context.checklists().stream()
			.filter(checklist -> "DAY".equals(checklist.scopeType()) && dayId.equals(checklist.itineraryDayId()))
			.map(AiTripContext.ChecklistSummary::id)
			.findFirst()
			.orElse(null);
	}

	private UUID checklistIdForTrip(AiTripContext context) {
		if (context == null) return null;
		return context.checklists().stream()
			.filter(checklist -> "TRIP".equals(checklist.scopeType()))
			.map(AiTripContext.ChecklistSummary::id)
			.findFirst()
			.orElse(null);
	}

	private String dayTitle(AiTripContext.DaySummary day) {
		if (day.title() != null && !day.title().isBlank()) return day.title() + " 체크리스트";
		if (day.dayNumber() != null) return day.dayNumber() + "일차 체크리스트";
		return "일차 체크리스트";
	}

	private AiGuideReply failed(String action, RuntimeException exception) {
		return new AiGuideReply(
			action + "을 처리하지 못했어요. AI 분석 서버가 일시적으로 원활하지 않을 수 있어요. 잠시 후 다시 시도해주세요.",
			List.<AiToolCall>of()
		);
	}

	private boolean greeting(String question) {
		return normalize(question).matches("(ㅎㅇ|안녕.*|반가워.*|하이|헬로|hi|hello)");
	}

	private String normalize(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[\\s!?.,~]+", "");
	}
}

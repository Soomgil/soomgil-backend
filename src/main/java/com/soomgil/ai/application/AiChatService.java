package com.soomgil.ai.application;

import com.soomgil.ai.api.dto.AiChatMessage;
import com.soomgil.ai.api.dto.AiChatSession;
import com.soomgil.ai.api.dto.AiMessageResponse;
import com.soomgil.ai.api.dto.AiMessageRole;
import com.soomgil.ai.api.dto.PagedAiChatMessage;
import com.soomgil.ai.infrastructure.persistence.AiChatMapper;
import com.soomgil.ai.infrastructure.persistence.AiChatMessageRow;
import com.soomgil.ai.infrastructure.persistence.AiChatSessionRow;
import com.soomgil.auth.application.handler.FindDisplayNameQueryHandler;
import com.soomgil.auth.application.query.FindDisplayNameQuery;
import com.soomgil.common.api.dto.OffsetPageMeta;
import com.soomgil.geo.api.dto.Viewport;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.trip.application.query.handler.TripAccessGuard;
import com.soomgil.user.api.dto.UserSummary;
import java.net.URI;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiChatService {

	private static final List<String> SORT = List.of("createdAt,desc", "id,desc");
	/** 이 아래면 분류기를 신뢰하지 않고 되묻는다. */
	private static final double LOW_CONFIDENCE = 0.35;
	/** 표현 근거가 없어도 삭제를 실행할 만큼 분류기가 확신한 기준. */
	private static final double HIGH_CONFIDENCE = 0.75;

	private final TripAccessGuard accessGuard;
	private final AiChatMapper mapper;
	private final AiGuideModel model;
	private final AiTripContextService contextService;
	private final FindDisplayNameQueryHandler displayNameHandler;
	private final SimpMessagingTemplate messagingTemplate;

	public AiChatService(
		TripAccessGuard accessGuard,
		AiChatMapper mapper,
		AiGuideModel model,
		AiTripContextService contextService,
		FindDisplayNameQueryHandler displayNameHandler,
		SimpMessagingTemplate messagingTemplate
	) {
		this.accessGuard = Objects.requireNonNull(accessGuard);
		this.mapper = Objects.requireNonNull(mapper);
		this.model = Objects.requireNonNull(model);
		this.contextService = Objects.requireNonNull(contextService);
		this.displayNameHandler = Objects.requireNonNull(displayNameHandler);
		this.messagingTemplate = Objects.requireNonNull(messagingTemplate);
	}

	@Transactional
	public AiChatSession getSession(UUID tripId, UUID userId) {
		accessGuard.requireActiveMember(tripId, userId);
		return toDto(requireSession(tripId));
	}

	@Transactional
	public PagedAiChatMessage listMessages(UUID tripId, UUID userId, int offset, int limit) {
		accessGuard.requireActiveMember(tripId, userId);
		validatePage(offset, limit);
		AiChatSessionRow session = requireSession(tripId);
		List<AiChatMessageRow> rows = mapper.findMessages(session.id(), offset, limit + 1);
		boolean hasMore = rows.size() > limit;
		return new PagedAiChatMessage(
			rows.stream().limit(limit).map(this::toDto).toList(),
			new OffsetPageMeta(offset, limit, hasMore ? offset + limit : null, hasMore, SORT)
		);
	}

	public AiMessageResponse createMessage(
		UUID tripId,
		UUID userId,
		String content,
		Long baseVersion
	) {
		return createMessage(tripId, userId, content, baseVersion, null);
	}

	public AiMessageResponse createMessage(
		UUID tripId,
		UUID userId,
		String content,
		Long baseVersion,
		Viewport viewport
	) {
		accessGuard.requireActiveMember(tripId, userId);
		String question = content == null ? "" : content.trim();
		if (question.isEmpty() || question.length() > 4000) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED);
		}
		AiChatSessionRow session = requireSession(tripId);
		List<AiGuideRequest.AiGuideTurn> recent = new ArrayList<>(mapper.findRecentMessages(session.id(), 20).stream()
			.map(row -> new AiGuideRequest.AiGuideTurn(row.role(), row.content()))
			.toList());
		UUID requestMessageId = UUID.randomUUID();
		mapper.insertMessage(requestMessageId, session.id(), userId, AiMessageRole.USER.name(), question, Instant.now());
		messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/ai", toDto(mapper.findMessageById(requestMessageId)));
		AiGuideRequest classificationRequest = new AiGuideRequest(
			tripId, userId, session.id(), requestMessageId, session.summary(), recent,
			question, baseVersion, viewport, null
		);
		AiIntentDecision decision = applySafetyPolicy(question, model.classify(classificationRequest));
		AiGuideRequest replyRequest = decision.intent().usesReadTools() || decision.intent().usesWriteTools()
			? withTripContext(classificationRequest, contextService.load(tripId, userId))
			: classificationRequest;
		AiGuideReply reply = switch (decision.intent().risk()) {
			case READ -> model.replyWithReadTools(replyRequest, decision);
			case REVERSIBLE, DESTRUCTIVE -> model.replyWithWriteTools(replyRequest, decision);
			case NONE -> model.replyWithoutTools(replyRequest, decision);
		};
		String answer = AiPlainTextFormatter.format(reply.content());
		if (decision.intent() == AiIntent.UNSUPPORTED
			&& !answer.contains(AiPlainTextFormatter.UNSUPPORTED_NOTICE)) {
			answer = answer + " " + AiPlainTextFormatter.UNSUPPORTED_NOTICE;
		}
		if (answer == null || answer.isBlank()) {
			throw new BusinessException(ErrorCode.AI_PROVIDER_UNAVAILABLE, "AI provider returned an empty response.");
		}
		UUID assistantMessageId = UUID.randomUUID();
		mapper.insertMessage(
			assistantMessageId, session.id(), null, AiMessageRole.ASSISTANT.name(), answer.trim(), Instant.now()
		);
		AiChatMessage assistant = toDto(mapper.findMessageById(assistantMessageId));
		for (var toolCall : reply.toolCalls()) {
			mapper.linkToolCallToResultMessage(toolCall.id(), assistantMessageId);
		}
		Long resultingVersion = reply.toolCalls().stream()
			.map(call -> call.versionAfter())
			.filter(Objects::nonNull)
			.max(Long::compareTo)
			.orElse(baseVersion);
		boolean undoAvailable = reply.toolCalls().stream()
			.anyMatch(call -> Boolean.TRUE.equals(call.undoRedoAvailable()));
		AiMessageResponse response = new AiMessageResponse(
			assistant, reply.toolCalls(), resultingVersion, undoAvailable, false
		);
		messagingTemplate.convertAndSend("/topic/trips/" + tripId + "/ai", response);
		return response;
	}

	private AiGuideRequest withTripContext(AiGuideRequest request, AiTripContext tripContext) {
		return new AiGuideRequest(
			request.tripId(), request.requesterUserId(), request.sessionId(), request.requestMessageId(),
			request.sessionSummary(), request.recentMessages(), request.question(), request.baseVersion(),
			request.viewport(), tripContext
		);
	}

	private AiIntentDecision validDecision(AiIntentDecision classified) {
		if (classified != null) return classified;
		return new AiIntentDecision(
			AiIntent.AMBIGUOUS, 0.0, "분류 결과가 없습니다.",
			"어떤 여행 정보를 확인하거나 변경하고 싶은지 조금 더 구체적으로 알려주시겠어요?"
		);
	}

	private AiIntentDecision applySafetyPolicy(String question, AiIntentDecision classified) {
		AiIntentDecision decision = classified == null
			? new AiIntentDecision(AiIntent.AMBIGUOUS, 0.0, "분류 결과가 없습니다.", null)
			: classified;
		String normalized = question.toLowerCase()
			.replaceAll("[\\s!?.,~]+", "")
			.trim();
		if (normalized.matches("(ㅎㅇ|안녕|안녕하세요|안녕하십니까|반가워|반가워요|반갑습니다|하이|헬로|hi|hello)")) {
			return decision.force(AiIntent.GENERAL_CHAT, "단순 인사는 도구를 사용하지 않습니다.");
		}
		if (normalized.matches("(고마워|고마워요|고맙습니다|감사|감사해|감사해요|감사합니다|thanks|thankyou)")) {
			return decision.force(AiIntent.GENERAL_CHAT, "단순 감사는 도구를 사용하지 않습니다.");
		}
		if (normalized.matches(".*(뭐할수있어|무엇을할수있어|어떤걸할수있어|사용법|기능알려줘|howtouse).*")) {
			return decision.force(AiIntent.HELP, "사용법 질문은 도구를 사용하지 않습니다.");
		}
		if (isRecommendedPlaceAddRequest(normalized)) {
			return decision.force(
				AiIntent.ADD_RECOMMENDED_PLACES_TO_ITINERARY,
				"장소 이름 없이 여행지 개수를 지정한 추가 요청은 추천 장소 추가로 처리합니다."
			);
		}
		if (isDeleteItineraryItemRequest(normalized)) {
			return decision.force(
				AiIntent.DELETE_ITINERARY_ITEM,
				"장소 이름 삭제 요청은 일정 장소 삭제 도구로 처리합니다."
			);
		}
		// 저장된 내용을 묻는 질문이 생성 규칙에 걸려 항목을 새로 만들지 않도록 조회를 먼저 판정한다.
		if (isPlanningReadRequest(normalized)) {
			return decision.force(
				AiIntent.READ_PLANNING,
				"저장된 메모·체크리스트 내용을 묻는 질문은 조회로 처리합니다."
			);
		}
		if (isChecklistGenerationRequest(normalized)) {
			return decision.force(
				AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY,
				"일정 기반 체크리스트 생성 요청은 자동 생성 도구로 처리합니다."
			);
		}
		// "4일차 추가해줘"는 장소 추가로 분류되기 쉬우나 일차 그룹을 만드는 요청이다.
		if (isDayManagementRequest(normalized)) {
			return decision.force(
				AiIntent.MANAGE_ITINERARY_DAY,
				"일차 자체를 늘리거나 고치는 요청은 일차 관리 도구로 처리합니다."
			);
		}
		// 이동수단을 지정한 경로 연결은 순서 재배치(OPTIMIZE_ROUTE)와 결과가 전혀 다르므로 분리한다.
		if (isRouteConnectionRequest(normalized)) {
			return decision.force(
				AiIntent.CONNECT_DAY_ROUTES,
				"이동수단을 지정한 경로 연결 요청은 경로 연결 도구로 처리합니다."
			);
		}
		return applyRiskGate(decision, normalized);
	}

	/**
	 * intent 위험도에 따라 실행 문턱을 다르게 적용한다.
	 *
	 * <p>예전에는 모든 도구 intent에 대해 하드코딩된 한국어 표현이 일치해야만 실행했다. 그 결과
	 * 분류기가 정확히 맞혀도 표현이 조금만 달라지면 되묻기로 떨어져 기능이 동작하지 않는 것처럼
	 * 보였다. 조회는 데이터를 바꾸지 않으므로 통과시키고, 되돌리기 부담이 큰 삭제 계열만
	 * 표현 근거 또는 높은 확신도를 요구한다.
	 */
	private AiIntentDecision applyRiskGate(AiIntentDecision decision, String normalized) {
		AiIntent intent = decision.intent();
		double confidence = decision.confidence();
		return switch (intent.risk()) {
			case NONE -> decision;
			// 조회는 잘못 분류돼도 데이터가 바뀌지 않는다. 되묻기보다 답을 주는 편이 낫다.
			case READ -> confidence < LOW_CONFIDENCE
				? clarify(decision, "조회 의도를 확신하지 못했습니다.")
				: decision;
			// 가역 변경은 undo가 가능하다. 표현 근거가 있으면 낮은 확신도라도 실행한다.
			case REVERSIBLE -> confidence < LOW_CONFIDENCE && !hasExplicitIntentCue(intent, normalized)
				? clarify(decision, "변경 의도를 확신하지 못했습니다.")
				: decision;
			// 삭제는 표현 근거가 있거나 분류기가 충분히 확신할 때만 실행한다.
			case DESTRUCTIVE -> hasExplicitIntentCue(intent, normalized) || confidence >= HIGH_CONFIDENCE
				? decision
				: clarify(decision, "삭제 요청으로 단정하기에 근거가 부족합니다.");
		};
	}

	private AiIntentDecision clarify(AiIntentDecision decision, String reason) {
		return new AiIntentDecision(
			AiIntent.AMBIGUOUS,
			decision.confidence(),
			reason,
			decision.clarificationQuestion() == null
				? "어떤 정보를 확인하거나 변경하고 싶은지 조금 더 구체적으로 알려주시겠어요?"
				: decision.clarificationQuestion()
		);
	}

	private boolean hasExplicitIntentCue(AiIntent intent, String question) {
		return switch (intent) {
			case READ_ITINERARY -> question.matches(".*(일정|일차|동선|경로).*(보여|조회|알려|확인|어떻게|뭐야).*"
				) || question.matches(".*(보여|조회|알려|확인).*(일정|일차|동선|경로).*");
			case SEARCH_PLACES -> question.matches(".*(찾아|검색|찾아줘|어디있|장소알려).*");
			case RECOMMEND_PLACES -> question.matches(".*(추천|어디갈|어디가좋|갈만한).*");
			case WRITE_NOTE -> question.contains("메모")
				&& question.matches(".*(써|작성|기록|추가|수정|바꿔|저장).*");
			case WRITE_CHECKLIST -> question.contains("체크리스트")
				&& question.matches(".*(만들|작성|추가|수정|바꿔|넣어|체크).*");
			case ADD_PLACE_TO_ITINERARY -> question.matches(".*(일정|일차).*(추가|넣어|등록).*")
				|| question.matches(".*(추가|넣어|등록).*(일정|일차).*");
			case ADD_RECOMMENDED_PLACES_TO_ITINERARY -> question.matches(".*(추천|갈만한|여행지|장소).*(넣어|추가|등록|일정에).*")
				|| question.matches(".*(넣어|추가|등록).*(추천|갈만한|여행지|장소).*")
				|| isRecommendedPlaceAddRequest(question);
			case DELETE_ITINERARY_ITEM -> isDeleteItineraryItemRequest(question);
			case MOVE_ITINERARY_ITEM -> question.matches(".*(옮겨|이동|재배치|순서.*바꿔).*");
			case SUMMARIZE_ITINERARY -> question.matches(".*(요약|정리|분석|리뷰|코스.*봐줘|코스.*리뷰).*")
				|| question.matches(".*(여행일정|여행.*일정|전체.*일정).*(어때|어떨까|봐줘).*");
			case READ_PLANNING -> question.matches(".*(체크리스트|준비물|메모).*(뭐|알려|보여|조회|확인|있어|남았).*")
				|| question.matches(".*(보여|알려|조회|확인).*(체크리스트|준비물|메모).*");
			case FILTER_PLACES_BY_CONDITION -> question.matches(".*(유료|무료|장애인|휠체어|유모차|접근|휴무|닫은|폐업|입장료).*(빼|삭제|제거|없애).*")
				|| question.matches(".*(빼|삭제|제거|없애).*(유료|무료|장애인|휠체어|유모차|접근|입장료).*");
			case GENERATE_CHECKLIST_FROM_ITINERARY -> question.matches(".*(체크리스트.*(자동|만들어|생성|추천|분석)|"
				+ "준비물.*알려|필요.*준비|예약.*필요.*체크).*")
				|| isChecklistGenerationRequest(question);
			case OPTIMIZE_ROUTE -> question.matches(".*(동선|이동순서|이동경로).*(최적화|정리|개선|재구성|짜|묶).*")
				|| question.matches(".*(최적화|정리|개선|재구성|묶).*(동선|이동순서|이동경로).*")
				|| question.matches(".*가까운.*(묶|같이|모아).*")
				|| question.matches(".*효율.*동선.*");
			case CONNECT_DAY_ROUTES -> isRouteConnectionRequest(question);
			case MANAGE_ITINERARY_DAY -> isDayManagementRequest(question);
			default -> false;
		};
	}

	/**
	 * 이동수단을 지정한 경로 연결 요청인지 판별한다.
	 *
	 * <p>"자전거로 2일차 이어줘"처럼 이동수단만 말하고 연결 동사를 생략하는 경우가 많아
	 * 이동수단 표현 단독으로도 근거로 인정한다.
	 */
	/**
	 * 일차 그룹 자체를 늘리거나 고치는 요청인지 판별한다.
	 *
	 * <p>장소 추가("2일차에 경복궁 넣어줘")와 구분해야 하므로, 일차를 가리키는 표현과
	 * 생성·수정 동사가 함께 있고 장소 추가 표현이 없을 때만 인정한다.
	 */
	private boolean isDayManagementRequest(String question) {
		boolean dayCreation = question.matches(".*\\d+일차.*(추가|만들|생성|늘려).*")
			|| question.matches(".*(하루|일차).*(더|추가|만들|생성|늘려).*");
		boolean dayEdit = question.matches(".*\\d+일차.*(이름|제목|날짜).*(바꿔|수정|변경|로).*")
			|| question.matches(".*(이름|제목|날짜).*\\d+일차.*(바꿔|수정|변경).*");
		return (dayCreation || dayEdit) && !question.matches(".*(장소|여행지|맛집|카페).*");
	}

	private boolean isRouteConnectionRequest(String question) {
		boolean connectVerb = question.matches(".*(연결|이어|이어서|길로|경로로|루트).*");
		boolean transportMode = question.matches(".*(자전거|바이크|bike|cycling|도보|걸어|걷|walk|자동차|차량|운전|car|driving).*");
		return connectVerb && (transportMode || question.matches(".*(일차|일정|장소).*"))
			|| transportMode && question.matches(".*(경로|길|동선|이동).*");
	}

	/**
	 * 이미 저장된 메모·체크리스트 내용을 묻는 질문인지 판별한다.
	 *
	 * <p>"체크리스트에 뭐 있어?"는 조회인데 생성 규칙의 "알려"에 걸려 항목을 새로 만들어버리던 문제가
	 * 있었다. 저장된 내용을 가리키는 표현이 있으면 생성보다 조회를 우선한다.
	 */
	private boolean isPlanningReadRequest(String question) {
		// "메모에 ...라고 써줘"처럼 저장할 내용에 조회 표현이 섞여 있을 수 있어 쓰기 동사가 있으면 제외한다.
		if (question.matches(".*(써줘|써|적어|작성|기록|저장|수정|바꿔|만들|생성|넣어).*")) {
			return false;
		}
		return question.matches(".*(체크리스트|메모).*(뭐있|뭐가있|있는지|있어|있나|남았|보여|확인|어디까지).*")
			|| question.matches(".*(보여|확인).*(체크리스트|메모).*");
	}

	private boolean isChecklistGenerationRequest(String question) {
		if (isPlanningReadRequest(question)) {
			return false;
		}
		return question.matches(".*(체크리스트|준비물).*(자동|만들|생성|추천|분석|작성|짜).*")
			|| question.matches(".*(체크리스트|준비물).*(여행계획|여행|여행전|여행전준비|준비물).*(추가|넣어|작성).*")
			|| question.matches(".*(여행계획|여행|여행전|여행전준비|준비물).*(체크리스트).*(추가|넣어|작성).*")
			|| question.matches(".*체크리스트에.*(여행|여행전|준비물).*")
			|| question.matches(".*(자동|분석).*(체크리스트|준비물).*")
			|| question.matches(".*여행.*필요.*준비.*")
			|| question.matches(".*예약.*필요.*체크.*")
			|| question.matches(".*준비물.*뭐.*");
	}

	private boolean isRecommendedPlaceAddRequest(String question) {
		return question.matches(".*(여행지|장소|갈만한곳|갈곳).*\\d+개.*(추가|넣어|등록).*")
			|| question.matches(".*\\d+개.*(여행지|장소|갈만한곳|갈곳).*(추가|넣어|등록).*")
			|| question.matches(".*(추천|알아서).*(여행지|장소|갈만한곳|갈곳).*(추가|넣어|등록).*");
	}

	private boolean isDeleteItineraryItemRequest(String question) {
		return !isConditionBasedRemovalRequest(question)
			&& question.matches(".*(지워|삭제|제거|빼줘|빼기|없애).*");
	}

	private boolean isConditionBasedRemovalRequest(String question) {
		return question.matches(".*(유료|무료|장애인|휠체어|유모차|접근|휴무|닫은|폐업|입장료).*");
	}

	private AiChatSessionRow requireSession(UUID tripId) {
		AiChatSessionRow existing = mapper.findSessionByTripId(tripId);
		if (existing != null) {
			return existing;
		}
		mapper.insertSessionIfAbsent(UUID.randomUUID(), tripId, Instant.now());
		AiChatSessionRow created = mapper.findSessionByTripId(tripId);
		if (created == null) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "AI session could not be created.");
		}
		return created;
	}

	private void validatePage(int offset, int limit) {
		if (offset < 0 || limit < 1 || limit > 100) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "offset or limit is out of range.");
		}
	}

	private AiChatSession toDto(AiChatSessionRow row) {
		return new AiChatSession(row.id(), row.tripId(), row.status(), offset(row.summaryUpdatedAt()), offset(row.createdAt()));
	}

	private AiChatMessage toDto(AiChatMessageRow row) {
		if (row == null) {
			throw new BusinessException(ErrorCode.INTERNAL_ERROR, "Stored AI message could not be loaded.");
		}
		UserSummary requester = null;
		if (row.requesterUserId() != null) {
			FindDisplayNameQuery query = new FindDisplayNameQuery(row.requesterUserId());
			String name = row.requesterDisplayName() != null
				? row.requesterDisplayName() : displayNameHandler.handle(query);
			URI image = row.requesterProfileImageUrl() != null
				? URI.create(row.requesterProfileImageUrl()) : displayNameHandler.findProfileImageUrl(query);
			requester = new UserSummary(row.requesterUserId(), name, image);
		}
		return new AiChatMessage(
			row.id(), AiMessageRole.valueOf(row.role()), requester, row.content(), row.toolCallId(), offset(row.createdAt())
		);
	}

	private OffsetDateTime offset(Instant value) {
		return value == null ? null : OffsetDateTime.ofInstant(value, ZoneOffset.UTC);
	}
}

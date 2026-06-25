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
		AiGuideReply reply = switch (decision.intent()) {
			case READ_ITINERARY, SEARCH_PLACES, RECOMMEND_PLACES, SUMMARIZE_ITINERARY ->
				model.replyWithReadTools(replyRequest, decision);
			case WRITE_NOTE, WRITE_CHECKLIST, ADD_PLACE_TO_ITINERARY, ADD_RECOMMENDED_PLACES_TO_ITINERARY,
					DELETE_ITINERARY_ITEM, MOVE_ITINERARY_ITEM,
					FILTER_PLACES_BY_CONDITION, GENERATE_CHECKLIST_FROM_ITINERARY, OPTIMIZE_ROUTE ->
				model.replyWithWriteTools(replyRequest, decision);
			case GENERAL_CHAT, HELP, AMBIGUOUS, UNSUPPORTED ->
				model.replyWithoutTools(replyRequest, decision);
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
		if (isChecklistGenerationRequest(normalized)) {
			return decision.force(
				AiIntent.GENERATE_CHECKLIST_FROM_ITINERARY,
				"일정 기반 체크리스트 생성 요청은 자동 생성 도구로 처리합니다."
			);
		}
		if ((decision.intent().usesReadTools() || decision.intent().usesWriteTools())
			&& !hasExplicitIntentCue(decision.intent(), normalized)) {
			return new AiIntentDecision(
				AiIntent.AMBIGUOUS,
				decision.confidence(),
				"도구 실행에 필요한 명시적인 요청 표현이 없습니다.",
				"조회하거나 변경하려는 내용을 조금 더 구체적으로 말씀해주시겠어요?"
			);
		}
		if (decision.confidence() < 0.55 && decision.intent() != AiIntent.GENERAL_CHAT
			&& decision.intent() != AiIntent.HELP && decision.intent() != AiIntent.UNSUPPORTED) {
			return new AiIntentDecision(
				AiIntent.AMBIGUOUS,
				decision.confidence(),
				"분류 확신도가 낮아 실행하지 않습니다.",
				decision.clarificationQuestion() == null
					? "어떤 정보를 확인하거나 변경하고 싶은지 조금 더 구체적으로 알려주시겠어요?"
					: decision.clarificationQuestion()
			);
		}
		return decision;
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
			case FILTER_PLACES_BY_CONDITION -> question.matches(".*(유료|무료|장애인|유모차|접근|휴무|닫은|폐업).*(빼|삭제|제거|없애).*")
				|| question.matches(".*(빼|삭제|제거|없애).*(유료|무료|장애인|유모카|접근).*");
			case GENERATE_CHECKLIST_FROM_ITINERARY -> question.matches(".*(체크리스트.*(자동|만들어|생성|추천|분석)|"
				+ "준비물.*알려|필요.*준비|예약.*필요.*체크).*")
				|| isChecklistGenerationRequest(question);
			case OPTIMIZE_ROUTE -> question.matches(".*(동선.*최적화|최적화.*동선|가까운.*곳.*묶어|동선.*정리|"
				+ "이동.*순서.*정리|효율.*동선).*");
			default -> false;
		};
	}

	private boolean isChecklistGenerationRequest(String question) {
		return question.matches(".*(체크리스트|준비물).*(자동|만들|생성|추천|분석|작성|알려|짜).*")
			|| question.matches(".*(자동|분석).*(체크리스트|준비물).*")
			|| question.matches(".*여행.*필요.*준비.*")
			|| question.matches(".*예약.*필요.*체크.*")
			|| question.matches(".*준비물.*뭐.*")
			|| question.matches(".*체크리스트.*뭐.*");
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

package com.soomgil.voting.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.global.security.CurrentUser;
import com.soomgil.voting.api.dto.CloseVoteSessionRequest;
import com.soomgil.voting.api.dto.MyVoteStickerState;
import com.soomgil.voting.api.dto.OpenVoteSessionRequest;
import com.soomgil.voting.api.dto.SaveVoteStickersRequest;
import com.soomgil.voting.api.dto.SubmitVoteRequest;
import com.soomgil.voting.api.dto.TripVoteSessionDetail;
import com.soomgil.voting.api.dto.TripVoteSessionResult;
import com.soomgil.voting.api.dto.TripVoteSessionState;
import com.soomgil.voting.api.dto.VoteStickerPlacement;
import com.soomgil.voting.application.command.dto.CloseVoteSessionCommand;
import com.soomgil.voting.application.command.dto.OpenVoteSessionCommand;
import com.soomgil.voting.application.command.dto.SaveMyVoteStickersCommand;
import com.soomgil.voting.application.command.dto.SubmitMyVoteCommand;
import com.soomgil.voting.application.command.dto.VoteStickerPlacementCommand;
import com.soomgil.voting.application.command.handler.CloseVoteSessionHandler;
import com.soomgil.voting.application.command.handler.OpenVoteSessionHandler;
import com.soomgil.voting.application.command.handler.SaveMyVoteStickersHandler;
import com.soomgil.voting.application.command.handler.SubmitMyVoteHandler;
import com.soomgil.voting.application.query.dto.FindCurrentVoteSessionQuery;
import com.soomgil.voting.application.query.dto.FindVoteSessionResultQuery;
import com.soomgil.voting.application.query.handler.FindCurrentVoteSessionHandler;
import com.soomgil.voting.application.query.handler.FindVoteSessionResultHandler;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 여행 방 스티커 투표 REST 엔드포인트.
 *
 * <p>모든 엔드포인트는 여행방 active member 인증이 필요하다. 투표 시작과 조기 종료는 방장만 호출할 수 있고,
 * 스티커 저장과 제출은 투표 시작 시점에 확정된 참여자만 호출할 수 있다.
 *
 * <p>{@code GET /vote-sessions/current}는 프론트 라우터 가드가 쓰는 단일 진입점으로,
 * 지도 화면보다 투표 화면을 먼저 보여줄지 서버가 판정해 {@code nextScreen}으로 알려준다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/trips/{tripId}/vote-sessions")
@SecurityRequirement(name = "bearerAuth")
public class TripVoteSessionController extends ApiControllerSupport {

	private final OpenVoteSessionHandler openVoteSessionHandler;
	private final SaveMyVoteStickersHandler saveMyVoteStickersHandler;
	private final SubmitMyVoteHandler submitMyVoteHandler;
	private final CloseVoteSessionHandler closeVoteSessionHandler;
	private final FindCurrentVoteSessionHandler findCurrentVoteSessionHandler;
	private final FindVoteSessionResultHandler findVoteSessionResultHandler;

	public TripVoteSessionController(
		OpenVoteSessionHandler openVoteSessionHandler,
		SaveMyVoteStickersHandler saveMyVoteStickersHandler,
		SubmitMyVoteHandler submitMyVoteHandler,
		CloseVoteSessionHandler closeVoteSessionHandler,
		FindCurrentVoteSessionHandler findCurrentVoteSessionHandler,
		FindVoteSessionResultHandler findVoteSessionResultHandler
	) {
		this.openVoteSessionHandler = openVoteSessionHandler;
		this.saveMyVoteStickersHandler = saveMyVoteStickersHandler;
		this.submitMyVoteHandler = submitMyVoteHandler;
		this.closeVoteSessionHandler = closeVoteSessionHandler;
		this.findCurrentVoteSessionHandler = findCurrentVoteSessionHandler;
		this.findVoteSessionResultHandler = findVoteSessionResultHandler;
	}

	/**
	 * 여행 방 진입 시 보여줄 화면과 현재 투표 상태를 조회한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param currentUser 인증 사용자
	 * @return 진입 화면 판정과 세션/참여 상태
	 */
	@GetMapping("/current")
	public TripVoteSessionState getCurrentSession(
		@PathVariable UUID tripId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return findCurrentVoteSessionHandler.handle(
			new FindCurrentVoteSessionQuery(tripId, currentUser.userId())
		);
	}

	/**
	 * 투표를 시작한다. 방장만 호출할 수 있다.
	 *
	 * @param tripId 여행방 식별자
	 * @param currentUser 인증 사용자
	 * @param request 스티커 지급 개수와 선정 개수
	 * @return 생성된 세션 상세
	 */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TripVoteSessionDetail openSession(
		@PathVariable UUID tripId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody OpenVoteSessionRequest request
	) {
		return openVoteSessionHandler.handle(new OpenVoteSessionCommand(
			tripId,
			currentUser.userId(),
			request.stickerAllowance(),
			request.selectionCount(),
			request.candidateCount()
		));
	}

	/**
	 * 제출 전 스티커 배치를 저장한다. 배치 전체를 치환하므로 이동과 회수도 이 엔드포인트로 표현한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param sessionId 세션 식별자
	 * @param currentUser 인증 사용자
	 * @param request 저장할 배치 전체
	 * @return 저장 후 참여 상태
	 */
	@PutMapping("/{sessionId}/my-stickers")
	public MyVoteStickerState saveMyStickers(
		@PathVariable UUID tripId,
		@PathVariable UUID sessionId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody SaveVoteStickersRequest request
	) {
		return saveMyVoteStickersHandler.handle(new SaveMyVoteStickersCommand(
			tripId, sessionId, currentUser.userId(), toPlacementCommands(request.placements())
		));
	}

	/**
	 * 투표를 제출한다. 제출 후에는 수정할 수 없으며, 마지막 참여자의 제출이면 세션이 자동 종료된다.
	 *
	 * @param tripId 여행방 식별자
	 * @param sessionId 세션 식별자
	 * @param currentUser 인증 사용자
	 * @param request 함께 저장할 배치. 생략 가능
	 * @return 제출 후 세션과 참여 상태
	 */
	@PostMapping("/{sessionId}/my-submission")
	public TripVoteSessionState submitMyVote(
		@PathVariable UUID tripId,
		@PathVariable UUID sessionId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody(required = false) SubmitVoteRequest request
	) {
		List<VoteStickerPlacementCommand> placements = request == null || request.placements() == null
			? null
			: toPlacementCommands(request.placements());
		return submitMyVoteHandler.handle(new SubmitMyVoteCommand(
			tripId, sessionId, currentUser.userId(), placements
		));
	}

	/**
	 * 방장이 투표를 조기 종료한다. 이미 종료된 세션에 호출해도 같은 결과를 반환하는 멱등 연산이다.
	 *
	 * @param tripId 여행방 식별자
	 * @param sessionId 세션 식별자
	 * @param currentUser 인증 사용자
	 * @param request 미투표자 경고 확인 여부
	 * @return 확정된 결과
	 */
	@PostMapping("/{sessionId}/completion")
	public TripVoteSessionResult closeSession(
		@PathVariable UUID tripId,
		@PathVariable UUID sessionId,
		@AuthenticationPrincipal CurrentUser currentUser,
		@Valid @RequestBody CloseVoteSessionRequest request
	) {
		return closeVoteSessionHandler.handle(new CloseVoteSessionCommand(
			tripId, sessionId, currentUser.userId(), request.acknowledgeUnvotedParticipants()
		));
	}

	/**
	 * 종료된 투표의 결과를 조회한다.
	 *
	 * @param tripId 여행방 식별자
	 * @param sessionId 세션 식별자
	 * @param currentUser 인증 사용자
	 * @return 후보별 결과와 일정 반영 상태
	 */
	@GetMapping("/{sessionId}/result")
	public TripVoteSessionResult getResult(
		@PathVariable UUID tripId,
		@PathVariable UUID sessionId,
		@AuthenticationPrincipal CurrentUser currentUser
	) {
		return findVoteSessionResultHandler.handle(
			new FindVoteSessionResultQuery(tripId, sessionId, currentUser.userId())
		);
	}

	private List<VoteStickerPlacementCommand> toPlacementCommands(List<VoteStickerPlacement> placements) {
		if (placements == null) {
			return List.of();
		}
		return placements.stream()
			.map(placement -> new VoteStickerPlacementCommand(placement.candidateId(), placement.stickerCount()))
			.toList();
	}
}

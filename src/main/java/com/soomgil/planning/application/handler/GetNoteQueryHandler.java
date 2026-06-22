package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.planning.api.dto.Note;
import com.soomgil.planning.application.query.GetNoteQuery;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.NoteRecord;
import com.soomgil.planning.domain.model.PlanningException;
import com.soomgil.planning.domain.policy.PlanningPolicy;
import com.soomgil.planning.infrastructure.persistence.mapper.NoteMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

/**
 * {@link GetNoteQuery}를 처리한다.
 *
 * <p>(trip, scope, day) 조합으로 활성 note를 찾아 DTO로 조립한다.
 * scope/day 일관성을 먼저 검증하고, note가 없으면
 * {@link ErrorCode#PLANNING_NOTE_NOT_FOUND}.
 */
@Component
@Transactional(readOnly = true)
public class GetNoteQueryHandler implements QueryHandler<GetNoteQuery, Note> {

	private final NoteMapper noteMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;

	public GetNoteQueryHandler(
		NoteMapper noteMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker
	) {
		this.noteMapper = noteMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
	}

	@Override
	public Note handle(GetNoteQuery query) {
		return findOptional(query)
			.orElseThrow(() -> new PlanningException(ErrorCode.PLANNING_NOTE_NOT_FOUND));
	}

	/**
	 * 메모가 선택 정보인 내부 조회에서 예외로 트랜잭션을 rollback-only 처리하지 않도록 한다.
	 */
	public Optional<Note> findOptional(GetNoteQuery query) {
		accessChecker.requireMember(query.tripId(), query.viewerUserId());
		PlanningPolicy.validateScopeDay(query.scopeType(), query.itineraryDayId());

		return noteMapper.findByTripScopeDay(query.tripId(), query.scopeType(), query.itineraryDayId())
			.map(assembler::toNoteDto);
	}
}

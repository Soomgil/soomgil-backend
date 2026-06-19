package com.soomgil.planning.application.handler;

import com.soomgil.common.cqrs.QueryHandler;
import com.soomgil.planning.api.dto.Checklist;
import com.soomgil.planning.application.query.ListChecklistsQuery;
import com.soomgil.planning.application.service.PlanningAssembler;
import com.soomgil.planning.application.service.TripMemberAccessChecker;
import com.soomgil.planning.domain.model.ChecklistItemRecord;
import com.soomgil.planning.domain.model.ChecklistMemberStatusRecord;
import com.soomgil.planning.domain.model.ChecklistRecord;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistItemMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMemberStatusMapper;
import com.soomgil.planning.infrastructure.persistence.mapper.ChecklistMapper;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link ListChecklistsQuery}를 처리한다.
 *
 * <p>{@code scopeType}/{@code itineraryDayId} 필터는 optional. 둘 다 null이면 trip의 모든 활성
 * checklist를 반환한다. 각 checklist는 종속 items와 member statuses를 함께 조립한다.
 *
 * <p>planning은 trip당 소수 checklist이므로 N+1 조회를 허용한다.
 */
@Component
@Transactional(readOnly = true)
public class ListChecklistsQueryHandler implements QueryHandler<ListChecklistsQuery, List<Checklist>> {

	private final ChecklistMapper checklistMapper;
	private final ChecklistItemMapper itemMapper;
	private final ChecklistMemberStatusMapper statusMapper;
	private final PlanningAssembler assembler;
	private final TripMemberAccessChecker accessChecker;

	public ListChecklistsQueryHandler(
		ChecklistMapper checklistMapper,
		ChecklistItemMapper itemMapper,
		ChecklistMemberStatusMapper statusMapper,
		PlanningAssembler assembler,
		TripMemberAccessChecker accessChecker
	) {
		this.checklistMapper = checklistMapper;
		this.itemMapper = itemMapper;
		this.statusMapper = statusMapper;
		this.assembler = assembler;
		this.accessChecker = accessChecker;
	}

	@Override
	public List<Checklist> handle(ListChecklistsQuery query) {
		accessChecker.requireMember(query.tripId(), query.viewerUserId());

		List<ChecklistRecord> checklists = checklistMapper.findByTripIdWithFilters(
			query.tripId(), query.scopeType(), query.itineraryDayId());

		return checklists.stream()
			.map(record -> {
				List<ChecklistItemRecord> items = itemMapper.findByChecklistId(record.id());
				Map<UUID, List<ChecklistMemberStatusRecord>> statusesByItem = new HashMap<>();
				for (ChecklistItemRecord item : items) {
					statusesByItem.put(item.id(), statusMapper.findByItemId(item.id()));
				}
				return assembler.toChecklistDto(record, items, statusesByItem);
			})
			.toList();
	}
}

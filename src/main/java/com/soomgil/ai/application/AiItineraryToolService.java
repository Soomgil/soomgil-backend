package com.soomgil.ai.application;

import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.DeleteItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.DeleteItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 일정 도구의 복합 변경을 기존 itinerary handler로 조정한다.
 *
 * <p>일차 미정 그룹 생성과 장소 추가를 같은 transaction에서 처리하므로 두 번째 변경이
 * 실패하면 임시 day도 함께 rollback된다. 직접 저장소나 mapper를 호출하지 않는다.
 */
@Service
public class AiItineraryToolService {

	private final FindItineraryHandler itineraryHandler;
	private final CreateItineraryDayHandler createDayHandler;
	private final CreateItineraryItemHandler createItemHandler;
	private final DeleteItineraryItemHandler deleteItemHandler;
	private final UpdateItineraryItemHandler updateItemHandler;

	public AiItineraryToolService(
		FindItineraryHandler itineraryHandler,
		CreateItineraryDayHandler createDayHandler,
		CreateItineraryItemHandler createItemHandler,
		DeleteItineraryItemHandler deleteItemHandler,
		UpdateItineraryItemHandler updateItemHandler
	) {
		this.itineraryHandler = itineraryHandler;
		this.createDayHandler = createDayHandler;
		this.createItemHandler = createItemHandler;
		this.deleteItemHandler = deleteItemHandler;
		this.updateItemHandler = updateItemHandler;
	}

	/**
	 * 장소를 지정한 day에 추가하고, day가 없으면 일차 미정 그룹을 재사용하거나 생성한다.
	 *
	 * @param tripId 변경할 여행방 ID
	 * @param userId 변경을 요청한 active member ID
	 * @param baseVersion 변경 전 itinerary version
	 * @param input 추가할 장소와 정렬 정보
	 * @return 장소 추가 후 itinerary mutation 결과
	 */
	@Transactional
	public ItineraryMutationResult addPlace(
		UUID tripId,
		UUID userId,
		long baseVersion,
		AddPlaceInput input
	) {
		UUID dayId = input.itineraryDayId();
		long itemBaseVersion = baseVersion;
		if (dayId == null) {
			var itinerary = itineraryHandler.handle(new FindItineraryQuery(tripId, userId));
			var unscheduled = itinerary.days().stream()
				.filter(day -> day.groupType() == ItineraryDayGroupType.UNSCHEDULED)
				.findFirst();
			if (unscheduled.isPresent()) {
				dayId = unscheduled.get().id();
			}
			else {
				ItineraryMutationResult createdDay = createDayHandler.handle(new CreateItineraryDayCommand(
					tripId, userId, baseVersion, ItineraryDayGroupType.UNSCHEDULED,
					null, null, "일차 미정", 0
				));
				dayId = createdDay.day().id();
				itemBaseVersion = createdDay.itineraryVersion();
			}
		}
		return createItemHandler.handle(new CreateItineraryItemCommand(
			tripId, userId, itemBaseVersion, dayId, input.sortOrder(), ItineraryItemType.PLACE,
			input.placeProvider(), input.externalPlaceId(), input.placeName(), input.address(),
			input.lat(), input.lng(), input.thumbnailUrl()
		));
	}

	/**
	 * LLM이 판별한 조건(장애인 이용 불가, 유모차 진입 불가, 유료 시설 등)에 해당하는
	 * 일정 항목들을 한 번에 삭제한다. 각 삭제는 baseVersion을 앞선 결과로 갱신해 순차 적용.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 요청자 ID
	 * @param baseVersion 시작 itinerary version
	 * @param itemIds 삭제할 item ID 목록
	 * @return 마지막으로 적용된 itinerary mutation 결과. itemIds가 비었으면 null.
	 */
	@Transactional
	public ItineraryMutationResult removeItems(
		UUID tripId,
		UUID userId,
		long baseVersion,
		List<UUID> itemIds
	) {
		if (itemIds == null || itemIds.isEmpty()) return null;
		long version = baseVersion;
		ItineraryMutationResult last = null;
		for (UUID itemId : itemIds) {
			last = deleteItemHandler.handle(new DeleteItineraryItemCommand(
				tripId, userId, version, itemId
			));
			version = last.itineraryVersion();
		}
		return last;
	}

	/**
	 * 동선 최적화 등으로 여러 item을 다른 일차로 옮기고 정렬한다. 각 이동은 앞선 결과로
	 * baseVersion을 갱신해 순차 적용.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 요청자 ID
	 * @param baseVersion 시작 itinerary version
	 * @param moves 이동할 항목 목록
	 * @return 마지막으로 적용된 itinerary mutation 결과. moves가 비었으면 null.
	 */
	@Transactional
	public ItineraryMutationResult reorderItems(
		UUID tripId,
		UUID userId,
		long baseVersion,
		List<ItemMove> moves
	) {
		if (moves == null || moves.isEmpty()) return null;
		long version = baseVersion;
		ItineraryMutationResult last = null;
		List<UUID> affectedItemIds = new ArrayList<>();
		for (ItemMove move : moves) {
			last = updateItemHandler.handle(new UpdateItineraryItemCommand(
				tripId, userId, version, move.itemId(), move.itineraryDayId(),
				move.sortOrder(), move.placeName(), move.address(), move.lat(), move.lng(),
				move.thumbnailUrl() == null ? null : URI.create(move.thumbnailUrl())
			));
			version = last.itineraryVersion();
			affectedItemIds.add(move.itemId());
		}
		return last;
	}

	/** AI 장소 추가 도구가 itinerary 모듈에 전달하는 입력. */

	/** AI 장소 추가 도구가 itinerary 모듈에 전달하는 입력. */
	public record AddPlaceInput(
		UUID itineraryDayId,
		int sortOrder,
		String placeProvider,
		String externalPlaceId,
		String placeName,
		String address,
		Double lat,
		Double lng,
		URI thumbnailUrl
	) {
	}

	/** 동선 최적화 도구가 전달하는 단일 항목 이동 정보. */
	public record ItemMove(
		UUID itemId,
		UUID itineraryDayId,
		Integer sortOrder,
		String placeName,
		String address,
		Double lat,
		Double lng,
		String thumbnailUrl
	) {
	}
}

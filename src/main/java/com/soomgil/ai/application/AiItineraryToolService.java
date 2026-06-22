package com.soomgil.ai.application;

import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.DeleteItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.dto.ItineraryDayOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemOrderCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryItemView;
import com.soomgil.itinerary.application.command.dto.ReorderItineraryCommand;
import com.soomgil.itinerary.application.command.dto.UpdateItineraryItemCommand;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.DeleteItineraryItemHandler;
import com.soomgil.itinerary.application.command.handler.ReorderItineraryHandler;
import com.soomgil.itinerary.application.command.handler.UpdateItineraryItemHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
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
	private final ReorderItineraryHandler reorderItineraryHandler;

	public AiItineraryToolService(
		FindItineraryHandler itineraryHandler,
		CreateItineraryDayHandler createDayHandler,
		CreateItineraryItemHandler createItemHandler,
		DeleteItineraryItemHandler deleteItemHandler,
		UpdateItineraryItemHandler updateItemHandler,
		ReorderItineraryHandler reorderItineraryHandler
	) {
		this.itineraryHandler = itineraryHandler;
		this.createDayHandler = createDayHandler;
		this.createItemHandler = createItemHandler;
		this.deleteItemHandler = deleteItemHandler;
		this.updateItemHandler = updateItemHandler;
		this.reorderItineraryHandler = reorderItineraryHandler;
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
	 * 사용자가 지정한 ID 또는 장소명으로 최신 일정 항목을 찾아 삭제한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 요청자 ID
	 * @param baseVersion 변경 전 itinerary version
	 * @param itemId 모델이 현재 맥락에서 확인한 item ID
	 * @param placeName 사용자가 말한 장소명
	 * @return 삭제 결과
	 */
	@Transactional
	public ItineraryMutationResult deleteItem(
		UUID tripId,
		UUID userId,
		long baseVersion,
		UUID itemId,
		String placeName
	) {
		var itinerary = itineraryHandler.handle(new FindItineraryQuery(tripId, userId));
		ItineraryItemView item = resolveItem(itinerary, itemId, placeName);
		return deleteItemHandler.handle(new DeleteItineraryItemCommand(
			tripId, userId, baseVersion, item.id()
		));
	}

	/**
	 * 장소명과 목표 일차를 최신 일정에서 해석해 전체 순서 snapshot으로 안전하게 이동한다.
	 *
	 * @param tripId 여행방 ID
	 * @param userId 요청자 ID
	 * @param baseVersion 변경 전 itinerary version
	 * @param itemId 모델이 현재 맥락에서 확인한 item ID
	 * @param placeName 사용자가 말한 장소명
	 * @param targetDayId 모델이 현재 맥락에서 확인한 목표 day ID
	 * @param targetDayNumber 사용자가 말한 목표 일차 번호
	 * @param targetSortOrder 목표 일차에서의 순서. null이면 마지막
	 * @return 이동 결과
	 */
	@Transactional
	public ItineraryMutationResult moveItem(
		UUID tripId,
		UUID userId,
		long baseVersion,
		UUID itemId,
		String placeName,
		UUID targetDayId,
		Integer targetDayNumber,
		Integer targetSortOrder
	) {
		var itinerary = itineraryHandler.handle(new FindItineraryQuery(tripId, userId));
		ItineraryItemView item = resolveItem(itinerary, itemId, placeName);
		UUID resolvedDayId = resolveDayId(itinerary, targetDayId, targetDayNumber);
		List<ItineraryDayOrderCommand> order = itinerary.days().stream()
			.sorted(Comparator.comparingInt(day -> day.sortOrder() == null ? Integer.MAX_VALUE : day.sortOrder()))
			.map(day -> {
				List<UUID> itemIds = new ArrayList<>(day.items().stream()
					.sorted(Comparator.comparingInt(ItineraryItemView::sortOrder))
					.map(ItineraryItemView::id)
					.filter(id -> !id.equals(item.id()))
					.toList());
				if (day.id().equals(resolvedDayId)) {
					int index = targetSortOrder == null
						? itemIds.size() : Math.max(0, Math.min(targetSortOrder, itemIds.size()));
					itemIds.add(index, item.id());
				}
				List<ItineraryItemOrderCommand> items = new ArrayList<>();
				for (int index = 0; index < itemIds.size(); index++) {
					items.add(new ItineraryItemOrderCommand(itemIds.get(index), index));
				}
				return new ItineraryDayOrderCommand(
					day.id(), day.sortOrder() == null ? 0 : day.sortOrder(), items
				);
			})
			.toList();
		return reorderItineraryHandler.handle(new ReorderItineraryCommand(
			tripId, userId, baseVersion, order
		));
	}

	private ItineraryItemView resolveItem(
		com.soomgil.itinerary.application.query.dto.ItineraryView itinerary,
		UUID itemId,
		String placeName
	) {
		List<ItineraryItemView> items = itinerary.days().stream().flatMap(day -> day.items().stream()).toList();
		if (itemId != null) {
			return items.stream().filter(item -> item.id().equals(itemId)).findFirst()
				.orElseThrow(() -> new BusinessException(
					ErrorCode.RESOURCE_NOT_FOUND, "일정에서 해당 장소를 찾지 못했어요."
				));
		}
		String target = normalizeName(placeName);
		if (target.isEmpty()) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "삭제하거나 이동할 장소 이름이 필요해요.");
		}
		List<ItineraryItemView> exact = items.stream()
			.filter(item -> normalizeName(item.placeName()).equals(target)).toList();
		if (exact.size() == 1) return exact.getFirst();
		List<ItineraryItemView> partial = items.stream()
			.filter(item -> {
				String candidate = normalizeName(item.placeName());
				return candidate.contains(target) || target.contains(candidate);
			})
			.toList();
		if (partial.size() == 1) return partial.getFirst();
		if (exact.size() > 1 || partial.size() > 1) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "같은 이름의 장소가 여러 개예요. 일차도 함께 알려주세요.");
		}
		throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "일정에서 '" + placeName + "'을(를) 찾지 못했어요.");
	}

	private UUID resolveDayId(
		com.soomgil.itinerary.application.query.dto.ItineraryView itinerary,
		UUID dayId,
		Integer dayNumber
	) {
		if (dayId != null) {
			return itinerary.days().stream().filter(day -> day.id().equals(dayId)).map(day -> day.id()).findFirst()
				.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "목표 일차를 찾지 못했어요."));
		}
		if (dayNumber == null) {
			throw new BusinessException(ErrorCode.VALIDATION_FAILED, "몇 일차로 옮길지 알려주세요.");
		}
		return itinerary.days().stream()
			.filter(day -> day.dayNumber() != null && day.dayNumber().equals(dayNumber))
			.map(day -> day.id())
			.findFirst()
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, dayNumber + "일차를 찾지 못했어요."));
	}

	private String normalizeName(String value) {
		return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
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

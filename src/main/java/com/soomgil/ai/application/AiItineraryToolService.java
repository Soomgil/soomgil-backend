package com.soomgil.ai.application;

import com.soomgil.itinerary.application.command.dto.CreateItineraryDayCommand;
import com.soomgil.itinerary.application.command.dto.CreateItineraryItemCommand;
import com.soomgil.itinerary.application.command.dto.ItineraryMutationResult;
import com.soomgil.itinerary.application.command.handler.CreateItineraryDayHandler;
import com.soomgil.itinerary.application.command.handler.CreateItineraryItemHandler;
import com.soomgil.itinerary.application.query.dto.FindItineraryQuery;
import com.soomgil.itinerary.application.query.handler.FindItineraryHandler;
import com.soomgil.itinerary.domain.model.ItineraryDayGroupType;
import com.soomgil.itinerary.domain.model.ItineraryItemType;
import java.net.URI;
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

	public AiItineraryToolService(
		FindItineraryHandler itineraryHandler,
		CreateItineraryDayHandler createDayHandler,
		CreateItineraryItemHandler createItemHandler
	) {
		this.itineraryHandler = itineraryHandler;
		this.createDayHandler = createDayHandler;
		this.createItemHandler = createItemHandler;
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
}

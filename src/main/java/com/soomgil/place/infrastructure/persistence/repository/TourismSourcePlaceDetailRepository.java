package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.api.dto.PlaceSourceStatus;
import com.soomgil.place.application.query.dto.PlaceDetailItem;
import com.soomgil.place.application.query.dto.PlaceDetailQuery;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourcePlaceDetailMapper;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceDetailRow;
import java.net.URI;
import org.springframework.stereotype.Repository;

/**
 * 관광 원천 저장소에서 장소 상세 정보를 조회하는 repository.
 */
@Repository
public class TourismSourcePlaceDetailRepository {

	private final TourismSourcePlaceDetailMapper mapper;

	public TourismSourcePlaceDetailRepository(TourismSourcePlaceDetailMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * provider와 외부 장소 id에 맞는 관광 원천 상세 정보를 조회한다.
	 *
	 * @param query 상세 조회 query
	 * @return 장소 상세 조회 결과
	 */
	public PlaceDetailItem find(PlaceDetailQuery query) {
		if (query.provider() != PlaceProvider.KTO) {
			throw notFound();
		}

		TourismSourcePlaceDetailRow row = mapper.findByContentId(query.externalPlaceId());
		if (row == null) {
			throw notFound();
		}

		return new PlaceDetailItem(
			String.valueOf(row.contentId()),
			row.title(),
			row.address(),
			row.latitude(),
			row.longitude(),
			toUri(row.thumbnailUrl()),
			row.category(),
			PlaceSourceStatus.AVAILABLE,
			row.overview(),
			row.tel(),
			row.sourceModifiedAt(),
			false
		);
	}

	private BusinessException notFound() {
		return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "Place was not found.");
	}

	private URI toUri(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return URI.create(value);
	}
}

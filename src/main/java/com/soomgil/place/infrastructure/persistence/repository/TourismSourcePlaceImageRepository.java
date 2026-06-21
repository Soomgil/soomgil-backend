package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.place.api.dto.PlaceProvider;
import com.soomgil.place.application.query.dto.PlaceImageCandidate;
import com.soomgil.place.application.query.dto.PlaceImageCandidateQuery;
import com.soomgil.place.application.query.dto.PlaceImageCandidateType;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourcePlaceImageMapper;
import com.soomgil.place.infrastructure.persistence.row.TourismSourcePlaceImageRow;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 관광 원천 저장소에서 장소 이미지 후보를 조회하는 repository.
 */
@Repository
public class TourismSourcePlaceImageRepository {

	private final TourismSourcePlaceImageMapper mapper;

	public TourismSourcePlaceImageRepository(TourismSourcePlaceImageMapper mapper) {
		this.mapper = mapper;
	}

	/**
	 * 일반 관광지 이미지 최대 4장과 public serving 가능한 수상작 이미지 최대 1장을 조합한다.
	 *
	 * @param query 이미지 후보 조회 query
	 * @return 이미지 후보 목록
	 */
	public List<PlaceImageCandidate> findCandidates(PlaceImageCandidateQuery query) {
		if (query.provider() != PlaceProvider.KTO) {
			return List.of();
		}

		List<TourismSourcePlaceImageRow> rows = new ArrayList<>(mapper.findNormalImages(query.externalPlaceId()));
		TourismSourcePlaceImageRow awardImage = mapper.findAwardImage(query.externalPlaceId());
		if (awardImage != null) {
			rows.add(awardImage);
		}

		return rows.stream()
			.map(this::toCandidate)
			.toList();
	}

	private PlaceImageCandidate toCandidate(TourismSourcePlaceImageRow row) {
		return new PlaceImageCandidate(
			PlaceImageCandidateType.valueOf(row.imageType()),
			URI.create(row.publicUrl()),
			row.sourceType(),
			row.displayOrder(),
			row.width(),
			row.height()
		);
	}
}

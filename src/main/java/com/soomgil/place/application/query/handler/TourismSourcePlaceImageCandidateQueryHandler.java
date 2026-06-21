package com.soomgil.place.application.query.handler;

import com.soomgil.place.application.query.dto.PlaceImageCandidate;
import com.soomgil.place.application.query.dto.PlaceImageCandidateQuery;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceImageRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 장소 이미지 후보 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceImageCandidateQueryHandler implements PlaceImageCandidateQueryHandler {

	private final TourismSourcePlaceImageRepository repository;

	public TourismSourcePlaceImageCandidateQueryHandler(TourismSourcePlaceImageRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<PlaceImageCandidate> handle(PlaceImageCandidateQuery query) {
		return repository.findCandidates(query);
	}
}

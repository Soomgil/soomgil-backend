package com.soomgil.place.application.query.handler;

import com.soomgil.place.application.query.dto.PlaceViewportCandidate;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateCriteria;
import com.soomgil.place.application.query.dto.PlaceViewportCandidateQuery;
import com.soomgil.place.infrastructure.persistence.repository.TourismSourcePlaceSearchRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 관광 원천 데이터를 사용해 viewport 장소 후보 query를 처리하는 handler.
 */
@Service
public class TourismSourcePlaceViewportCandidateQueryHandler implements PlaceViewportCandidateQueryHandler {

	private final TourismSourcePlaceSearchRepository repository;

	public TourismSourcePlaceViewportCandidateQueryHandler(TourismSourcePlaceSearchRepository repository) {
		this.repository = repository;
	}

	@Override
	public List<PlaceViewportCandidate> handle(PlaceViewportCandidateQuery query) {
		return repository.findViewportCandidates(new PlaceViewportCandidateCriteria(
			query.bbox(),
			query.category(),
			query.limit()
		));
	}
}

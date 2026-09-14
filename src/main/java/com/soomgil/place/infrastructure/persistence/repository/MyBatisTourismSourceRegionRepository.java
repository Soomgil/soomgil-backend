package com.soomgil.place.infrastructure.persistence.repository;

import com.soomgil.place.application.port.TourismSourceRegionRepository;
import com.soomgil.place.infrastructure.persistence.mapper.TourismSourceRegionMapper;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * MyBatis 기반 관광 원천 지역 코드 저장소.
 */
@Repository
public class MyBatisTourismSourceRegionRepository implements TourismSourceRegionRepository {
	private final TourismSourceRegionMapper mapper;

	public MyBatisTourismSourceRegionRepository(TourismSourceRegionMapper mapper) {
		this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
	}

	@Override
	public Optional<Integer> findGugunCode(int sidoCode, String gugunName) {
		if (gugunName == null || gugunName.isBlank()) {
			return Optional.empty();
		}
		return Optional.ofNullable(mapper.findGugunCode(sidoCode, gugunName.strip()));
	}
}

package com.soomgil.tourismsource.matching.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

/**
 * 관광 원천 공모전 수상작 사진 매칭 SQL mapper.
 */
@Mapper
public interface TourismSourceContestAwardPhotoMatchingMapper {

	TourismSourceContestAwardPhotoRow findPhoto(String photoId);

	List<TourismSourceRegionAliasRow> findActiveRegionAliases();

	List<TourismSourceAttractionMatchRow> findAttractions();

	boolean existsSelectedMatch(String photoId);

	void deleteGeneratedMatches(String photoId);

	void insertMatch(TourismSourceContestAwardPhotoMatchInsertRow row);
}

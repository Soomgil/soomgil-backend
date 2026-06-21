package com.soomgil.social.infrastructure.persistence.mapper;

import com.soomgil.place.api.dto.PlaceRef;
import com.soomgil.social.infrastructure.persistence.row.FolloweePlaceReactionRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 활성 팔로우 관계와 장소 반응을 조회하는 MyBatis mapper.
 */
@Mapper
public interface SocialFolloweePlaceReactionMapper {

	List<FolloweePlaceReactionRow> findPositiveReactions(
		@Param("currentUserId") String currentUserId,
		@Param("places") List<PlaceRef> places
	);
}

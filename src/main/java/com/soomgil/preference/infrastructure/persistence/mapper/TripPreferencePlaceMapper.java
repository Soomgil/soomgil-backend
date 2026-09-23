package com.soomgil.preference.infrastructure.persistence.mapper;

import com.soomgil.preference.api.dto.TripPreferencePlace;
import java.util.List;
import java.util.UUID;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 관광공사 외부 호출 없이 저장된 관광지와 공개 가능한 최종 반응을 조회한다. */
@Mapper
public interface TripPreferencePlaceMapper {
    /** 반응을 변경한 사용자가 현재 참여 중인 여행방을 찾는다. */
    List<UUID> findActiveTripIdsByUser(@Param("userId") String userId);
    /** 인증된 요청자와 권한 검사를 마친 ACTIVE 멤버만 전달한다. 최대 200개 장소의 반응을 반환한다. */
    List<TripPreferencePlace> find(
        @Param("viewerId") String viewerId, @Param("memberIds") List<String> memberIds,
        @Param("west") double west, @Param("south") double south,
        @Param("east") double east, @Param("north") double north);
}

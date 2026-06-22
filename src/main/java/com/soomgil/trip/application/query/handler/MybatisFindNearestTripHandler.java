package com.soomgil.trip.application.query.handler;

import com.soomgil.trip.api.dto.NearestTripDto;
import com.soomgil.trip.application.query.dto.FindNearestTripQuery;
import com.soomgil.trip.infrastructure.persistence.mapper.TripQueryMapper;
import com.soomgil.trip.infrastructure.persistence.row.NearestTripRow;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MybatisFindNearestTripHandler implements FindNearestTripHandler {

	private final TripQueryMapper mapper;

	public MybatisFindNearestTripHandler(TripQueryMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	@Transactional(readOnly = true)
	public NearestTripDto handle(FindNearestTripQuery query) {
		NearestTripRow row = mapper.findNearestTrip(query.userId());
		if (row == null) {
			return null;
		}

		// Since we don't fetch true user profiles yet, we'll return an empty list of thumbnails
		// and let the frontend render placeholders for memberCount number of members.
		List<String> memberThumbnails = List.of();

		return new NearestTripDto(
			UUID.fromString(row.id()),
			row.title(),
			row.displayDestination(),
			row.startDate(),
			row.memberCount(),
			memberThumbnails,
			row.coverImageUrl()
		);
	}
}

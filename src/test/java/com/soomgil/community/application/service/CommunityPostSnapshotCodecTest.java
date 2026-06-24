package com.soomgil.community.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.soomgil.community.api.dto.CommunityPostSnapshot;
import java.util.List;
import org.junit.jupiter.api.Test;

class CommunityPostSnapshotCodecTest {

	@Test
	void roundTripsAnImmutableSnapshot() {
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());
		CommunityPostSnapshot snapshot = new CommunityPostSnapshot(List.of(), List.of(), List.of(), List.of(), null);

		assertThat(codec.decode(codec.encode(snapshot))).isEqualTo(snapshot);
	}

	@Test
	void treatsMissingPlanningCollectionsAsEmptyLists() {
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());

		CommunityPostSnapshot snapshot = codec.decode("""
			{"days":[],"routes":[],"authorDisplay":null}
			""");

		assertThat(snapshot.notes()).isEmpty();
		assertThat(snapshot.checklists()).isEmpty();
	}

	@Test
	void decodesLegacyItineraryItemPlaceRefField() {
		CommunityPostSnapshotCodec codec = new CommunityPostSnapshotCodec(new ObjectMapper().findAndRegisterModules());

		CommunityPostSnapshot snapshot = codec.decode("""
			{
			  "days": [{
			    "id": "3305f8a5-5c03-393a-e1a6-7951302da2ff",
			    "tripId": "9045a1d4-0818-f353-aea3-3b38b6beabf0",
			    "groupType": "DAY",
			    "dayNumber": 1,
			    "sortOrder": 0,
			    "items": [{
			      "id": "34da9dda-02a5-a805-20c3-ecab3e7121b7",
			      "itineraryDayId": "3305f8a5-5c03-393a-e1a6-7951302da2ff",
			      "sortOrder": 0,
			      "itemType": "PLACE",
			      "placeRef": {"provider": "KTO", "externalPlaceId": "20001"},
			      "placeName": "국립중앙과학관",
			      "sourceStatus": "AVAILABLE"
			    }]
			  }],
			  "routes": [],
			  "authorDisplay": null
			}
			""");

		assertThat(snapshot.days().getFirst().items().getFirst().place().externalPlaceId()).isEqualTo("20001");
	}
}

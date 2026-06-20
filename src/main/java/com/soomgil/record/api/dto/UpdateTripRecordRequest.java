package com.soomgil.record.api.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 여행 기록 부분 수정 요청.
 *
 * <p>JSON에서 생략한 필드는 기존 값을 유지하고, 명시적인 {@code null}은 값을 제거한다.
 */
public final class UpdateTripRecordRequest {

	private UUID itineraryDayId;
	private UUID itineraryItemId;
	private String title;
	private String caption;
	private String locationName;
	private Double lat;
	private Double lng;
	private OffsetDateTime takenAt;
	private List<UUID> mediaFileIds;
	private boolean itineraryDayIdProvided;
	private boolean itineraryItemIdProvided;
	private boolean titleProvided;
	private boolean captionProvided;
	private boolean locationNameProvided;
	private boolean latProvided;
	private boolean lngProvided;
	private boolean takenAtProvided;
	private boolean mediaFileIdsProvided;

	public UpdateTripRecordRequest() {
	}

	public UpdateTripRecordRequest(
		UUID itineraryDayId,
		UUID itineraryItemId,
		String title,
		String caption,
		String locationName,
		Double lat,
		Double lng,
		OffsetDateTime takenAt,
		List<UUID> mediaFileIds
	) {
		setItineraryDayId(itineraryDayId);
		setItineraryItemId(itineraryItemId);
		setTitle(title);
		setCaption(caption);
		setLocationName(locationName);
		setLat(lat);
		setLng(lng);
		setTakenAt(takenAt);
		setMediaFileIds(mediaFileIds);
	}

	@JsonSetter("itineraryDayId")
	public void setItineraryDayId(UUID value) { itineraryDayId = value; itineraryDayIdProvided = true; }

	@JsonSetter("itineraryItemId")
	public void setItineraryItemId(UUID value) { itineraryItemId = value; itineraryItemIdProvided = true; }

	@JsonSetter("title")
	public void setTitle(String value) { title = value; titleProvided = true; }

	@JsonSetter("caption")
	public void setCaption(String value) { caption = value; captionProvided = true; }

	@JsonSetter("locationName")
	public void setLocationName(String value) { locationName = value; locationNameProvided = true; }

	@JsonSetter("lat")
	public void setLat(Double value) { lat = value; latProvided = true; }

	@JsonSetter("lng")
	public void setLng(Double value) { lng = value; lngProvided = true; }

	@JsonSetter("takenAt")
	public void setTakenAt(OffsetDateTime value) { takenAt = value; takenAtProvided = true; }

	@JsonSetter("mediaFileIds")
	public void setMediaFileIds(List<UUID> value) { mediaFileIds = value; mediaFileIdsProvided = true; }

	public UUID itineraryDayId() { return itineraryDayId; }
	public UUID itineraryItemId() { return itineraryItemId; }
	public String title() { return title; }
	public String caption() { return caption; }
	public String locationName() { return locationName; }
	public Double lat() { return lat; }
	public Double lng() { return lng; }
	public OffsetDateTime takenAt() { return takenAt; }
	public List<UUID> mediaFileIds() { return mediaFileIds; }
	public boolean itineraryDayIdProvided() { return itineraryDayIdProvided; }
	public boolean itineraryItemIdProvided() { return itineraryItemIdProvided; }
	public boolean titleProvided() { return titleProvided; }
	public boolean captionProvided() { return captionProvided; }
	public boolean locationNameProvided() { return locationNameProvided; }
	public boolean latProvided() { return latProvided; }
	public boolean lngProvided() { return lngProvided; }
	public boolean takenAtProvided() { return takenAtProvided; }
	public boolean mediaFileIdsProvided() { return mediaFileIdsProvided; }
}

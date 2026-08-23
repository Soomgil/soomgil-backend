package com.soomgil.itinerary.domain.policy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.soomgil.global.error.BusinessException;
import com.soomgil.global.error.ErrorCode;
import com.soomgil.itinerary.domain.model.DrawingType;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class MapDrawingObjectPolicyTest {

	private static final UUID MEDIA_ID = UUID.fromString("70000000-0000-0000-0000-000000000001");
	private static final Map<String, Object> TRANSFORM = Map.of(
		"centerLng", 127.0,
		"centerLat", 37.0,
		"widthMeters", 120,
		"heightMeters", 80,
		"rotationDeg", 15
	);

	private final MapDrawingObjectPolicy policy = new MapDrawingObjectPolicy();

	@Test
	void acceptsStickerAndImageWithExclusiveSubtypeFields() {
		policy.validate(DrawingType.STICKER, null, "HEART", TRANSFORM);
		policy.validate(DrawingType.IMAGE, MEDIA_ID, null, TRANSFORM);
	}

	@Test
	void rejectsMissingOrMixedSubtypeFields() {
		assertValidation(() -> policy.validate(DrawingType.STICKER, null, null, TRANSFORM));
		assertValidation(() -> policy.validate(DrawingType.IMAGE, null, null, TRANSFORM));
		assertValidation(() -> policy.validate(DrawingType.IMAGE, MEDIA_ID, "HEART", TRANSFORM));
		assertValidation(() -> policy.validate(DrawingType.LINE, MEDIA_ID, null, TRANSFORM));
	}

	@Test
	void rejectsInvalidMapSizedTransform() {
		assertValidation(() -> policy.validate(
			DrawingType.STICKER,
			null,
			"HEART",
			Map.of("centerLng", 127, "centerLat", 37, "widthMeters", 0, "heightMeters", 80, "rotationDeg", 0)
		));
		assertValidation(() -> policy.validate(
			DrawingType.IMAGE,
			MEDIA_ID,
			null,
			Map.of("centerLng", 127, "centerLat", 37, "widthMeters", 120, "heightMeters", -1, "rotationDeg", 0)
		));
		assertValidation(() -> policy.validate(
			DrawingType.STICKER,
			null,
			"HEART",
			Map.of("centerLng", 181, "centerLat", 37, "widthMeters", 120, "heightMeters", 80, "rotationDeg", 0)
		));
	}

	private void assertValidation(Runnable action) {
		assertThatThrownBy(action::run)
			.isInstanceOfSatisfying(BusinessException.class, exception ->
				org.assertj.core.api.Assertions.assertThat(exception.errorCode())
					.isEqualTo(ErrorCode.VALIDATION_FAILED));
	}
}

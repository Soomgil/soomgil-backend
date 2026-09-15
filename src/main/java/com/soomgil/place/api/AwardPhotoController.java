package com.soomgil.place.api;

import com.soomgil.place.api.dto.AwardPhoto;
import com.soomgil.place.application.query.dto.ListAwardPhotosQuery;
import com.soomgil.place.application.query.handler.ListAwardPhotosQueryHandler;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관광사진 공모전 수상작 조회 API.
 *
 * <p>공개 데이터만 다루므로 인증 없이 조회할 수 있다. 랜딩과 로그인 화면에서도 사용한다.
 */
@Validated
@RestController
@RequestMapping("/api/v1/award-photos")
public class AwardPhotoController {

	private final ListAwardPhotosQueryHandler listAwardPhotosQueryHandler;

	public AwardPhotoController(ListAwardPhotosQueryHandler listAwardPhotosQueryHandler) {
		this.listAwardPhotosQueryHandler = listAwardPhotosQueryHandler;
	}

	@GetMapping
	public List<AwardPhoto> listAwardPhotos(
		@RequestParam(defaultValue = "10") int limit,
		@RequestParam(required = false) String regionCode
	) {
		return listAwardPhotosQueryHandler.handle(new ListAwardPhotosQuery(limit, regionCode));
	}
}

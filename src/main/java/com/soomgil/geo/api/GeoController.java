package com.soomgil.geo.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.geo.api.dto.LegalRegionLevel;
import com.soomgil.geo.api.dto.PagedLegalRegion;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/geo")
public class GeoController extends ApiControllerSupport {

	@GetMapping("/legal-regions")
	public PagedLegalRegion listLegalRegions(
		@RequestParam(required = false) String query,
		@RequestParam(required = false) LegalRegionLevel level,
		@RequestParam(required = false) String parentCode,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		return notImplemented();
	}
}

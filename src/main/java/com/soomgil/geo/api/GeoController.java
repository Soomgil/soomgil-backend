package com.soomgil.geo.api;

import com.soomgil.common.api.ApiControllerSupport;
import com.soomgil.common.api.dto.PageMeta;
import com.soomgil.geo.api.dto.LegalRegionLevel;
import com.soomgil.geo.api.dto.PagedLegalRegion;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.application.query.dto.ListLegalRegionsQuery;
import com.soomgil.geo.application.query.dto.PagedLegalRegionView;
import com.soomgil.geo.application.query.handler.ListLegalRegionsHandler;
import java.util.List;
import java.util.Objects;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class GeoController extends ApiControllerSupport {

	private final ListLegalRegionsHandler listLegalRegionsHandler;

	public GeoController(ListLegalRegionsHandler listLegalRegionsHandler) {
		this.listLegalRegionsHandler = Objects.requireNonNull(
			listLegalRegionsHandler,
			"listLegalRegionsHandler must not be null"
		);
	}

	@GetMapping("/legal-regions")
	public PagedLegalRegion listLegalRegions(
		@RequestParam(name = "q", required = false) String query,
		@RequestParam(required = false) LegalRegionLevel level,
		@RequestParam(required = false) String parentCode,
		@RequestParam(required = false) Boolean isActive,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "20") int size,
		@RequestParam(required = false) List<String> sort
	) {
		PagedLegalRegionView result = listLegalRegionsHandler.handle(new ListLegalRegionsQuery(
			query,
			toDomainLevel(level),
			parentCode,
			isActive,
			page,
			size,
			sort
		));
		return toPagedLegalRegion(result);
	}

	private PagedLegalRegion toPagedLegalRegion(PagedLegalRegionView view) {
		return new PagedLegalRegion(
			view.items().stream().map(this::toLegalRegion).toList(),
			new PageMeta(view.page(), view.size(), view.totalElements(), view.totalPages(), view.sort())
		);
	}

	private com.soomgil.geo.api.dto.LegalRegion toLegalRegion(LegalRegionView view) {
		return new com.soomgil.geo.api.dto.LegalRegion(
			view.code(),
			view.name(),
			view.fullName(),
			LegalRegionLevel.valueOf(view.level().name()),
			view.parentCode(),
			view.active()
		);
	}

	private com.soomgil.geo.domain.model.LegalRegionLevel toDomainLevel(LegalRegionLevel level) {
		return level == null ? null : com.soomgil.geo.domain.model.LegalRegionLevel.valueOf(level.name());
	}
}

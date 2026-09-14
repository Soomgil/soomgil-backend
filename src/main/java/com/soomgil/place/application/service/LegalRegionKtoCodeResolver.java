package com.soomgil.place.application.service;

import com.soomgil.geo.application.query.dto.FindLegalRegionsByCodesQuery;
import com.soomgil.geo.application.query.dto.LegalRegionView;
import com.soomgil.geo.application.query.handler.FindLegalRegionsByCodesHandler;
import com.soomgil.place.application.port.KtoRegionCode;
import com.soomgil.place.application.port.TourismSourceRegionRepository;
import com.soomgil.place.domain.policy.LegalRegionKtoAreaPolicy;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * 법정동 코드 목록을 한국관광공사 지역 코드로 바꾼다.
 *
 * <p>시도는 {@link LegalRegionKtoAreaPolicy}로 바로 대응하고, 시군구는 geo의 공개 query로 이름을 얻은 뒤
 * 관광 원천 시군구 목록에서 같은 이름을 찾는다. 이름을 못 찾으면 시도 범위로 넓혀 빈 결과를 피한다.
 * 같은 결과는 한 번만 남기고 입력 순서를 유지한다.
 */
@Component
public class LegalRegionKtoCodeResolver {
	private final FindLegalRegionsByCodesHandler legalRegionsHandler;
	private final TourismSourceRegionRepository regionRepository;

	public LegalRegionKtoCodeResolver(
		FindLegalRegionsByCodesHandler legalRegionsHandler,
		TourismSourceRegionRepository regionRepository
	) {
		this.legalRegionsHandler = Objects.requireNonNull(legalRegionsHandler, "legalRegionsHandler must not be null");
		this.regionRepository = Objects.requireNonNull(regionRepository, "regionRepository must not be null");
	}

	/**
	 * 법정동 코드를 KTO area/sigungu 코드로 바꾼다.
	 *
	 * @param legalRegionCodes 10자리 법정동 코드 목록
	 * @return 중복을 제거한 KTO 지역 코드 목록. 대응하는 시도가 없는 코드는 건너뛴다
	 */
	public List<KtoRegionCode> resolve(List<String> legalRegionCodes) {
		if (legalRegionCodes == null || legalRegionCodes.isEmpty()) {
			return List.of();
		}
		LinkedHashSet<KtoRegionCode> resolved = new LinkedHashSet<>();
		// 시군구 코드 → areaCode. 입력 순서를 지키기 위해 LinkedHashMap을 쓴다.
		Map<String, String> sigunguLookups = new LinkedHashMap<>();
		List<Runnable> ordered = new ArrayList<>();

		for (String code : legalRegionCodes) {
			Optional<String> areaCode = LegalRegionKtoAreaPolicy.areaCodeOf(code);
			if (areaCode.isEmpty()) {
				continue;
			}
			if (LegalRegionKtoAreaPolicy.isSidoLevel(code)) {
				resolved.add(new KtoRegionCode(areaCode.get(), null));
				continue;
			}
			// 읍면동 코드가 들어와도 소속 시군구로 올려서 조회한다.
			sigunguLookups.putIfAbsent(code.substring(0, 5) + "00000", areaCode.get());
		}

		if (!sigunguLookups.isEmpty()) {
			Map<String, String> namesByCode = legalRegionsHandler
				.handle(new FindLegalRegionsByCodesQuery(List.copyOf(sigunguLookups.keySet())))
				.stream()
				.collect(Collectors.toMap(LegalRegionView::code, LegalRegionView::name, (a, b) -> a));
			for (Map.Entry<String, String> lookup : sigunguLookups.entrySet()) {
				String areaCode = lookup.getValue();
				String name = namesByCode.get(lookup.getKey());
				Optional<Integer> gugunCode = name == null
					? Optional.empty()
					: regionRepository.findGugunCode(Integer.parseInt(areaCode), name);
				resolved.add(new KtoRegionCode(areaCode, gugunCode.map(String::valueOf).orElse(null)));
			}
		}
		return List.copyOf(resolved);
	}
}

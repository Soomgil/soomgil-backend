package com.soomgil.place.domain.policy;

import static java.util.Map.entry;

import java.util.Map;
import java.util.Optional;

/**
 * 법정동 시도 코드(앞 2자리)를 한국관광공사 {@code areaCode}로 대응시키는 정책.
 *
 * <p>여행방과 투표는 법정동 코드로 지역을 저장하지만 관광 원천(KTO)은 자체 지역 코드를 쓴다. 두 체계는
 * 시도 단위에서만 안정적으로 1:1이므로 이 정책은 시도 대응만 담당하고, 시군구는 이름 기준으로 별도 조회한다.
 *
 * <p>강원(42→51)과 전북(45→52)은 특별자치도 전환 전후 코드를 모두 같은 areaCode로 이어 준다.
 */
public final class LegalRegionKtoAreaPolicy {
	private static final Map<String, String> AREA_CODE_BY_SIDO = Map.ofEntries(
		entry("11", "1"),   // 서울특별시
		entry("26", "6"),   // 부산광역시
		entry("27", "4"),   // 대구광역시
		entry("28", "2"),   // 인천광역시
		entry("29", "5"),   // 광주광역시
		entry("30", "3"),   // 대전광역시
		entry("31", "7"),   // 울산광역시
		entry("36", "8"),   // 세종특별자치시
		entry("41", "31"),  // 경기도
		entry("42", "32"),  // 강원도(구 코드)
		entry("51", "32"),  // 강원특별자치도
		entry("43", "33"),  // 충청북도
		entry("44", "34"),  // 충청남도
		entry("45", "37"),  // 전라북도(구 코드)
		entry("52", "37"),  // 전북특별자치도
		entry("46", "38"),  // 전라남도
		entry("47", "35"),  // 경상북도
		entry("48", "36"),  // 경상남도
		entry("50", "39")   // 제주특별자치도
	);

	private LegalRegionKtoAreaPolicy() {
	}

	/**
	 * 법정동 코드가 속한 시도의 KTO areaCode를 찾는다.
	 *
	 * @param legalRegionCode 10자리 법정동 코드
	 * @return areaCode. 형식이 다르거나 대응이 없으면 empty
	 */
	public static Optional<String> areaCodeOf(String legalRegionCode) {
		if (legalRegionCode == null || !legalRegionCode.matches("\\d{10}")) {
			return Optional.empty();
		}
		return Optional.ofNullable(AREA_CODE_BY_SIDO.get(legalRegionCode.substring(0, 2)));
	}

	/**
	 * 시도 단위 코드인지 판정한다. 시도 코드는 앞 2자리 뒤가 모두 0이다.
	 *
	 * @param legalRegionCode 10자리 법정동 코드
	 * @return 시도 코드면 true
	 */
	public static boolean isSidoLevel(String legalRegionCode) {
		return legalRegionCode != null && legalRegionCode.length() == 10 && legalRegionCode.substring(2).equals("00000000");
	}
}

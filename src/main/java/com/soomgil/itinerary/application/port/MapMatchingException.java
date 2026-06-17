package com.soomgil.itinerary.application.port;

/**
 * 외부 map matching provider 실패.
 */
public class MapMatchingException extends RuntimeException {

	private final String providerCode;

	public MapMatchingException(String providerCode, String message) {
		super(message);
		this.providerCode = providerCode;
	}

	/**
	 * provider가 반환한 실패 code.
	 *
	 * @return 실패 code
	 */
	public String providerCode() {
		return providerCode;
	}
}

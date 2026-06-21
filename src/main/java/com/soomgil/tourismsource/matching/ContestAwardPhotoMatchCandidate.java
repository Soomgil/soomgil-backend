package com.soomgil.tourismsource.matching;

import java.math.BigDecimal;

record ContestAwardPhotoMatchCandidate(
	Integer attractionNo,
	Integer sidoCode,
	Integer gugunCode,
	String matchScope,
	String matchMethod,
	BigDecimal confidence
) {
}

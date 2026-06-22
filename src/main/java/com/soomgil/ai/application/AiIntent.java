package com.soomgil.ai.application;

/** AI 가이드의 1차 분류 결과. 이 값이 2차 호출에 노출할 도구의 상한을 결정한다. */
public enum AiIntent {
	GENERAL_CHAT,
	HELP,
	AMBIGUOUS,
	UNSUPPORTED,
	READ_ITINERARY,
	SEARCH_PLACES,
	RECOMMEND_PLACES,
	WRITE_NOTE,
	WRITE_CHECKLIST,
	ADD_PLACE_TO_ITINERARY,
	MOVE_ITINERARY_ITEM;

	public boolean usesReadTools() {
		return this == READ_ITINERARY || this == SEARCH_PLACES || this == RECOMMEND_PLACES;
	}

	public boolean usesWriteTools() {
		return this == WRITE_NOTE || this == WRITE_CHECKLIST
			|| this == ADD_PLACE_TO_ITINERARY || this == MOVE_ITINERARY_ITEM;
	}
}

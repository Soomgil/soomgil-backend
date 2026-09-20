package com.soomgil.notification.api.dto;

import java.util.UUID;

/** 알림 이동 대상. 초대·투표 식별자와 알림 종류별 화면 경로를 담는다. */
public record NotificationPayload(UUID tripId, UUID inviteId, String inviteCode, String route, UUID voteSessionId) { }

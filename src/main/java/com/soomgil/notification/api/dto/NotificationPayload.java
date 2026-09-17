package com.soomgil.notification.api.dto;

import java.util.UUID;

/** 알림 이동 대상. 초대는 inviteId·inviteCode, 투표는 voteSessionId를 사용하며 나머지는 null이다. */
public record NotificationPayload(UUID tripId, UUID inviteId, String inviteCode, String route, UUID voteSessionId) { }

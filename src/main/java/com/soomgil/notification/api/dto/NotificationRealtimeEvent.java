package com.soomgil.notification.api.dto;

/** 사용자 알림함 변경을 알리는 STOMP 메시지. */
public record NotificationRealtimeEvent(String eventType) {
}

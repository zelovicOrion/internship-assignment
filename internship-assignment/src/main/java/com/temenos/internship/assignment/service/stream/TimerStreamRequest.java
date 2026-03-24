package com.temenos.internship.assignment.service.stream;

import java.util.Map;

public record TimerStreamRequest(
        String timerId,
        int delay,
        long createdAt,
        String callbackUrl,
        String csrfToken
) {
    public Map<String, String> toStreamMap() {
        return Map.of(
                "timerId", timerId,
                "delay", String.valueOf(delay),
                "createdAt", String.valueOf(createdAt),
                "callbackUrl", callbackUrl,
                "csrfToken", csrfToken
        );
    }
}

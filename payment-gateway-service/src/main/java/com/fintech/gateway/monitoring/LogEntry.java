package com.fintech.gateway.monitoring;

import java.time.Instant;

/**
 * DTO for log entries streamed via WebSocket.
 */
public record LogEntry(
    String timestamp,
    String level,
    String logger,
    String message,
    String thread
) {
    public static LogEntry of(String level, String logger, String message, String thread) {
        return new LogEntry(
            Instant.now().toString(),
            level,
            logger,
            message,
            thread
        );
    }
}

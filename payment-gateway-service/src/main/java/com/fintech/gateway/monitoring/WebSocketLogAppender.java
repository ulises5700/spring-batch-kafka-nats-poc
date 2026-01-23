package com.fintech.gateway.monitoring;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Custom Logback Appender that broadcasts logs via WebSocket.
 */
public class WebSocketLogAppender extends AppenderBase<ILoggingEvent> {

    @Override
    protected void append(ILoggingEvent event) {
        WebSocketLogBroadcaster broadcaster = WebSocketLogBroadcaster.getInstance();
        if (broadcaster != null) {
            // Filter to only broadcast application logs
            String loggerName = event.getLoggerName();
            if (loggerName.startsWith("com.fintech") || 
                loggerName.contains("springframework.kafka") ||
                loggerName.contains("nats")) {
                
                LogEntry entry = LogEntry.of(
                    event.getLevel().toString(),
                    shortenLogger(loggerName),
                    event.getFormattedMessage(),
                    event.getThreadName()
                );
                broadcaster.broadcast(entry);
            }
        }
    }

    private String shortenLogger(String logger) {
        if (logger == null) return "unknown";
        int lastDot = logger.lastIndexOf('.');
        return lastDot > 0 ? logger.substring(lastDot + 1) : logger;
    }
}

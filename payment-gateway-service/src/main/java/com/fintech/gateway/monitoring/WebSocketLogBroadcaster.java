package com.fintech.gateway.monitoring;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting logs via WebSocket.
 */
@Service
public class WebSocketLogBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;
    private static WebSocketLogBroadcaster instance;

    public WebSocketLogBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
        instance = this;
    }

    public void broadcast(LogEntry logEntry) {
        if (messagingTemplate != null) {
            messagingTemplate.convertAndSend("/topic/logs", logEntry);
        }
    }

    public static WebSocketLogBroadcaster getInstance() {
        return instance;
    }
}

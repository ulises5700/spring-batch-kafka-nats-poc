package com.fintech.gateway.controller;

import com.fintech.gateway.monitoring.MetricsService;
import com.fintech.gateway.monitoring.MetricsService.DashboardMetrics;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST Controller for dashboard metrics.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@EnableScheduling
public class DashboardController {

    private final MetricsService metricsService;
    private final SimpMessagingTemplate messagingTemplate;

    public DashboardController(MetricsService metricsService, 
                               SimpMessagingTemplate messagingTemplate) {
        this.metricsService = metricsService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping("/metrics")
    public DashboardMetrics getMetrics() {
        return metricsService.getMetrics();
    }

    /**
     * Push metrics updates every second via WebSocket.
     */
    @Scheduled(fixedRate = 1000)
    public void pushMetrics() {
        messagingTemplate.convertAndSend("/topic/metrics", metricsService.getMetrics());
    }
}

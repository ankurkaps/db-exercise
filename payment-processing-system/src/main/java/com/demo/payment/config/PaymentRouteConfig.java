package com.demo.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "payment.route")
@Data
public class PaymentRouteConfig {
    private Jms jms = new Jms();
    private Rest rest = new Rest();

    @Data
    public static class Jms {
        private String requestQueue = "jms:queue:broker.requests";
        private String responseQueue = "jms:queue:broker.responses";
    }

    @Data
    public static class Rest {
        private String host = "localhost";
        private String port = "8082";
        private String endpoint = "/api/v2/broker/process-payment";
    }
}

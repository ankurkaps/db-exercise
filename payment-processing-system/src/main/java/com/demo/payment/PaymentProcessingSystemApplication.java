package com.demo.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class PaymentProcessingSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentProcessingSystemApplication.class, args);
    }
}

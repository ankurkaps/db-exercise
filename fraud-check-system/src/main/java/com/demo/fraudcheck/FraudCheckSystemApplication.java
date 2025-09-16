package com.demo.fraudcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class FraudCheckSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(FraudCheckSystemApplication.class, args);
    }
}

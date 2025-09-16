package com.demo.broker.controller;

import org.apache.camel.ProducerTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.PaymentRequest;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api")
public class BrokerController {

    @Autowired
    private ProducerTemplate producerTemplate;

    @PostMapping("/v2/broker/process-payment")
    public ResponseEntity<FraudCheckResponse> processPaymentV2(@RequestBody PaymentRequest request) {

        log.info("Recieved REST Request. Payload {}", request);
        
        var response = producerTemplate.requestBody(
            "direct:processFraudCheckRest",
            request,
            FraudCheckResponse.class
        );

        log.info("Recieved fraud check response. Payload {}", response);

        return ResponseEntity.ok(response);
    }
}

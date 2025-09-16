package com.demo.payment.route;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.PaymentRequest;
import com.demo.payment.config.PaymentRouteConfig;
import com.demo.payment.exception.PaymentAlreadyExistsException;
import com.demo.payment.model.PaymentRecord;
import com.demo.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PaymentProcessingRoute extends RouteBuilder {

    @Autowired
    private PaymentRouteConfig routeConfig;

    @Autowired
    private JacksonDataFormat jacksonDataFormat;

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private PaymentService paymentService;

    @Override
    public void configure() throws Exception {
        log.info("Payment Route config: {}", routeConfig);
        
        // Ensure we use the same mapper used by Spring
        JacksonDataFormat jacksonFraudCheckResponseFormat = new JacksonDataFormat(FraudCheckResponse.class);
        jacksonFraudCheckResponseFormat.setObjectMapper(objectMapper);

        // REST configuration for Broker Rest Endpoint
        restConfiguration().component("netty-http")
            .host(routeConfig.getRest().getHost())
            .port(routeConfig.getRest().getPort());

        
        // Enhanced route with status tracking
        from("direct:processPaymentWithTracking").routeId("payment-with-tracking")
            .log("Processing payment with status tracking: ${body}")
            .process(exchange -> {
                PaymentRequest request = exchange.getIn().getBody(PaymentRequest.class);
                log.info("Starting fraud check for payment: {}", request.getTransactionId());
                
                paymentService.save(request);
            })
            .choice()
                .when(header("useRest").isEqualTo(true))
                    .to("direct:processPaymentRest")
                .otherwise()
                    .to("direct:processPaymentJms")
            .end()
            .process(exchange -> {
                // Update payment status based on fraud check response
                FraudCheckResponse fraudResponse = exchange.getIn().getBody(FraudCheckResponse.class);
                PaymentRecord paymentRecord = paymentService.processPayment(fraudResponse);
                exchange.getIn().setBody(paymentRecord);
            });

        // V1: JMS-based route
        from("direct:processPaymentJms").routeId("jms-route")
            .log("V1: Processing payment via JMS. JMSCorrelationID: ${header.JMSCorrelationID}")
            .marshal(jacksonDataFormat)
            .log("V1 Marshalled JSON: type=${body.class.name} | headers=${headers}\n${body}")
            .setHeader("Content-Type", constant(MediaType.APPLICATION_JSON_VALUE))
//            .to(routeConfig.getJms().getRequestQueue())
            .log("V1: Payment request sent to broker via JMS")
            .to("""
                    jms:queue:broker.requests\
                    ?exchangePattern=InOut\
                    &replyTo=jms:queue:broker.responses\
                    &replyToType=Shared\
                    &useMessageIDAsCorrelationID=true\
                    &requestTimeout=30s\
                    &jmsMessageType=Text
                    """)
            .log("V1 Response raw: type=${body.class.name} | headers=${headers}\n${body}")
            .unmarshal(jacksonFraudCheckResponseFormat)
            .log("V1 Response raw: type=${body.class.name} | headers=${headers}\n${body}")
            ;

        // V2: REST-based route
        from("direct:processPaymentRest").routeId("rest-route")
            .log("V2 REST: Processing payment via REST JMSCorrelationID: ${header.JMSCorrelationID}")
            .marshal(jacksonDataFormat)
            .log("V2 REST: Marshalled: ${body}")
            .log("V2 REST: Sending to broker via REST")
            .setHeader("Content-Type", constant(MediaType.APPLICATION_JSON_VALUE))
            .to("rest:post:" + routeConfig.getRest().getEndpoint())
            .log("V2 REST: Received fraud check Response: \n${body}")
            .unmarshal(jacksonFraudCheckResponseFormat)
            .log("V2 REST: Unmarshalled Response: \n${body}")
            ;

    }
}

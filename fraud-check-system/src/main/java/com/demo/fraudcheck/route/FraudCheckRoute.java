package com.demo.fraudcheck.route;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.PaymentRequest;
import com.demo.fraudcheck.service.FraudCheckService;

@Component
public class FraudCheckRoute extends RouteBuilder {

    @Value("${queue.fraudcheck.request}")
    private String fraudCheckRequestQueue;

    @Value("${queue.fraudcheck.response}")
    private String fraudCheckResponseQueue;

    @Autowired
    private FraudCheckService fraudCheckService;
    
    @Override
    public void configure() throws Exception {
        configureUsingJaxbDataformat();
    }

    public void configureUsingJaxbDataformat() throws Exception {
        // Configure JAXB context
        var jaxbRequestFormat = new JaxbDataFormat(PaymentRequest.class.getPackage().getName());
        var jaxbResponseFormat = new JaxbDataFormat(FraudCheckResponse.class.getPackage().getName());

        from("jms:queue:fraud.check.requests")
            .log("Received fraud check request: JMSCorrelationID: ${header.JMSCorrelationID}\n${body}")
            .log("type=${body.class.name} | headers=${headers}")
            .log("Inbound: CorrelationID=${header.JMSCorrelationID}, ReplyTo=${header.JMSReplyTo}, Type=${body.class.name}")
            .process(exchange -> {
                // Store correlation ID for response
                String correlationId = exchange.getIn().getHeader("JMSCorrelationID", String.class);
                exchange.setProperty("responseCorrelationId", correlationId);
            })
            .unmarshal(jaxbRequestFormat)
            .log("After unmarshal: type=${body.class.name}")
            .process(exchange -> {
                // Handle JAXBElement unwrapping
                Object body = exchange.getIn().getBody();
                if (body instanceof jakarta.xml.bind.JAXBElement) {
                    jakarta.xml.bind.JAXBElement<?> jaxbElement = (jakarta.xml.bind.JAXBElement<?>) body;
                    exchange.getIn().setBody(jaxbElement.getValue());
                    log.info("Unwrapped JAXBElement to: {}", jaxbElement.getValue().getClass().getSimpleName());
                }
            })
            .log("Before fraud check: type=${body.class.name}")
            .bean(fraudCheckService, "checkFraud")
            .log("After fraud check: type=${body.class.name}")
            .marshal(jaxbResponseFormat)
            .setHeader(CONTENT_TYPE, constant(APPLICATION_XML_VALUE))
            .setHeader("JMSCorrelationID", exchangeProperty("responseCorrelationId"))
            .log("Sending response to fraud.check.responses with correlation ID: ${header.JMSCorrelationID}\n${body}")
//            .to("jms:queue:fraud.check.responses")
            ;
    }
}

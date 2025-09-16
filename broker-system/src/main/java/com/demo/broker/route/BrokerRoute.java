package com.demo.broker.route;

import static org.springframework.http.HttpHeaders.CONTENT_TYPE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.http.MediaType.TEXT_PLAIN_VALUE;

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.converter.jaxb.JaxbDataFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.demo.common.model.FraudCheckResponse;
import com.demo.common.model.PaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class BrokerRoute extends RouteBuilder {

    @Value("${queue.fraudcheck.request}")
    private String fraudCheckRequestQueue;

    @Value("${queue.fraudcheck.response}")
    private String fraudCheckResponseQueue;

    @Value("${queue.payment.request}")
    private String paymentRequestQueue;

    @Value("${queue.payment.response}")
    private String paymentResponseQueue;
    
    
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void configure() throws Exception {
        configureUsingJaxbDataFormat();
    }

    public void configureUsingJaxbDataFormat() throws Exception {
        // Use same object mapper as spring boot for both request and response
        JacksonDataFormat jacksonRequestFormat = new JacksonDataFormat(PaymentRequest.class);
        jacksonRequestFormat.setObjectMapper(objectMapper);

        JacksonDataFormat jacksonResponseFormat = new JacksonDataFormat(FraudCheckResponse.class);
        jacksonResponseFormat.setObjectMapper(objectMapper);

        
        // Configure JAXB context
        var jaxbRequestFormat = new JaxbDataFormat(PaymentRequest.class.getPackage().getName());
        var jaxbResponseFormat = new JaxbDataFormat(FraudCheckResponse.class.getPackage().getName());

        onException(org.apache.camel.ExchangeTimedOutException.class)
            .handled(true)
            .setHeader(CONTENT_TYPE, constant(TEXT_PLAIN_VALUE))
            .setBody(simple("Timeout waiting for fraud check response"))
            .setHeader(org.springframework.http.HttpStatus.class.getName(), constant(504))
            .log("Timed out waiting on fraud.check.responses");

        // V1: Handle JMS requests from PPS
        from("jms:queue:broker.requests").routeId("broker.jms")
        .log("Received fraud check request: JMSCorrelationID: ${header.JMSCorrelationID}\n${body}")
        .log("type=${body.class.name} | headers=${headers}")
        .log("Inbound: CorrelationID=${header.JMSCorrelationID}, ReplyTo=${header.JMSReplyTo}, Type=${body.class.name}")
        .unmarshal(jacksonRequestFormat)
        .marshal(jaxbRequestFormat)
        .setHeader("broker-source", constant("REST"))
        .setHeader(CONTENT_TYPE, constant(APPLICATION_XML_VALUE))
        // Request–reply over JMS using a fixed reply queue
        .to("""
            jms:queue:fraud.check.requests\
            ?exchangePattern=InOut\
            &replyTo=queue:fraud.check.responses\
            &replyToType=Shared\
            &useMessageIDAsCorrelationID=true\
            &requestTimeout=30s\
            &jmsMessageType=Text
            """)
        .log("JMS reply arrived: type=${body.class.name} | headers=${headers}\n${body}")
        .unmarshal(jaxbResponseFormat)
        .log("V1 JMS reply unmarshalled: type=${body.class.name} | headers=${headers}\n${body}")
        .marshal(jacksonResponseFormat) // Convert back to JSON
        .log("V1 JMS reply marshalled: type=${body.class.name} | headers=${headers}\n${body}")
        ;

        
        // Service1 route: REST -> JMS (InOut on fixed reply queue) -> REST
        from("direct:processFraudCheckRest").routeId("broker.rest")
            .log("V2 REST In: type=${body.class.name} | headers=${headers}\n${body}")
            .marshal(jaxbRequestFormat)
            .setHeader("broker-source", constant("REST"))
            .setHeader(CONTENT_TYPE, constant(APPLICATION_XML_VALUE))
            // Request–reply over JMS using a fixed reply queue
            .to("""
                jms:queue:fraud.check.requests\
                ?exchangePattern=InOut\
                &replyTo=queue:fraud.check.responses\
                &replyToType=Shared\
                &useMessageIDAsCorrelationID=true\
                &requestTimeout=30s\
                &jmsMessageType=Text
                """)
            .log("JMS reply arrived: type=${body.class.name} | headers=${headers}")
            .unmarshal(jaxbResponseFormat)
            .log("V2 REST reply arrived: type=${body.class.name} | headers=${headers}\n${body}")
            ;
    }
   
}

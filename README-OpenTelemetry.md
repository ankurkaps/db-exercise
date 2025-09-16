# Payment Fraud Check System - OpenTelemetry Integration

A microservices solution with OpenTelemetry for complete observability, distributed tracing, and metrics collection using Docker Compose for simplified deployment.

## OpenTelemetry Architecture

Our solution uses the OpenTelemetry standard for observability with the following components:

```
┌─────────────────┐    ┌──────────────────┐    ┌─────────────────┐
│ Payment System  │───▶│ OpenTelemetry    │───▶│ Grafana Local   │
│ (Port: 8081)    │    │ Collector        │    │ (Port: 3000)    │
└─────────────────┘    │ (Port: 4317)     │    | Grafana Cloud   |
                       └──────────────────┘    | (Port: 16686)   |
┌─────────────────┐           │                └─────────────────┘
│ Broker System   │───────────┤                
│ (Port: 8082)    │           │                
└─────────────────┘           │                
                              │                
┌─────────────────┐           │                
│ Fraud System    │───────────┘
│ (Port: 8083)    │
└─────────────────┘
```

## Key OpenTelemetry Features

### Distributed Tracing
- End-to-end request tracing across all microservices
- Automatic instrumentation for Spring Boot, HTTP calls, JMS, and database operations
- Custom span annotations with `@WithSpan`
- Trace correlation in all log entries
- Grafana integration for trace visualization

### Metrics Collection
- Application metrics (HTTP requests, JVM metrics, custom business metrics)
- Infrastructure metrics via OpenTelemetry Collector
- Prometheus export for monitoring and alerting
- Service health monitoring

### Structured Logging
- Trace-correlated logs with trace/span IDs
- Structured audit events with metadata
- Cross-service log correlation
- OpenTelemetry log attributes

## Quick Start with OpenTelemetry

### Pre-Requisite: 

The setup uses secrets to run. Those must be exported to the environment before running docker compose


### Recommended: Docker Compose Deployment

The simplest way to run the entire system with full OpenTelemetry observability is using Docker Compose, which builds and starts all services automatically:

```bash
# Export environment vars
source .env

# Build and start everything with observability stack
docker-compose up -d

# This single command will:
# - Build all microservices from source
# - Start OpenTelemetry Collector
# - Start Grafana LGTM stack (observability backend)
# - Configure all OpenTelemetry instrumentation automatically
# - Set up service discovery and messaging infrastructure

# Verify all services are running
docker-compose ps

# View logs with trace correlation
docker-compose logs -f payment-processing-system
```


```bash

# Clear/Reset including the volumes
docker-compose down --volumes    to clear the volumes to reset

```

### Alternative: Manual Local Development

For developers who want to run services individually with custom configurations:

```bash
# Environment vars
source .env

# 1. Start infrastructure only
docker-compose up -d consul activemq otel-collector grafana

# 2. Build all modules locally
mvn clean install

# 3. Download OpenTelemetry Java agent (if not already present)
./download-otel-agent.sh

# 4. Start each service with OpenTelemetry instrumentation on cli or in ide
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=payment-service \
     -Dotel.exporter.otlp.endpoint=http://localhost:14318 \
     -jar payment-processing-system/target/payment-processing-system-1.0-SNAPSHOT.jar

java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=broker-service \
     -Dotel.exporter.otlp.endpoint=http://localhost:14318 \
     -jar broker-system/target/broker-system-1.0-SNAPSHOT.jar

java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=fraud-check-service \
     -Dotel.exporter.otlp.endpoint=http://localhost:14318 \
     -jar fraud-check-system/target/fraud-check-system-1.0-SNAPSHOT.jar
```

## Observability Stack Access

| Component | URL | Purpose |
|-----------|-----|---------|
| **Grafana Dashboard** | http://localhost:3000 | Complete observability (traces, metrics, logs) |
| **Prometheus Metrics** | http://localhost:18889 | Metrics collection endpoint |
| **OpenTelemetry Collector** | http://localhost:14317 (gRPC)<br>http://localhost:14318 (HTTP) | Telemetry data collection |
| **Consul UI** | http://localhost:8500 | Service discovery |
| **ActiveMQ Console** | http://localhost:8161 | Message broker monitoring |

## End-to-End Traceability Example

### Complete Request Journey
1. **Payment Service** receives HTTP request → **Span: `payment-processing`**
2. **Payment Service** validates payment → **Span: `payment-validation`**
3. **Payment Service** calls Broker Service → **Span: `http-call-broker`**
4. **Broker Service** converts JSON→XML → **Span: `json-xml-conversion`**
5. **Broker Service** sends JMS message → **Span: `jms-send-fraud-check`**
6. **Fraud Check Service** processes request → **Span: `fraud-check-processing`**
7. **Response flows back** with same trace ID

### Trace Correlation in Logs
```json
{
  "timestamp": "2024-01-01T10:15:30.123Z",
  "level": "INFO",
  "service": "payment-service", 
  "traceId": "4bf92f3577b34da6a3ce929d0e0e4736",
  "spanId": "00f067aa0ba902b7",
  "message": "AUDIT_EVENT: {\"eventType\":\"PAYMENT_RECEIVED\",\"transactionId\":\"550e8400-e29b-41d4-a716-446655440000\",\"component\":\"PAYMENT_SERVICE\",\"traceId\":\"4bf92f3577b34da6a3ce929d0e0e4736\",\"spanId\":\"00f067aa0ba902b7\"}"
}
```

## Testing with OpenTelemetry

### Test Payment and View Trace
```bash
# 1. Send payment request
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d @sample-requests/valid-payment.json

# 2. View trace in Grafana
# - Go to http://localhost:3000
# - Navigate to Explore → Tempo
# - Select "payment-service" traces
# - Click on trace to see complete request flow across all services

# 3. View metrics in Prometheus format
curl http://localhost:18889/metrics

# 4. Access Grafana dashboards for comprehensive monitoring
# - Application metrics
# - JVM metrics
# - Infrastructure metrics
# - Custom business metrics
```

## Custom Instrumentation

### Adding Custom Spans
```java
@RestController
public class PaymentController {
    
    @PostMapping("/payments")
    @WithSpan("payment-processing")  // Custom span name
    public ResponseEntity<String> processPayment(@RequestBody Payment payment) {
        
        // Get current span and add custom attributes
        Span span = Span.current();
        span.setAllAttributes(Attributes.builder()
            .put("payment.transaction.id", payment.getTransactionId())
            .put("payment.amount", payment.getAmount().toString())
            .put("payment.currency", payment.getCurrency())
            .build());
            
        // Business logic here
        return ResponseEntity.ok("Payment processed");
    }
}
```

### Custom Metrics
```java
@Service
public class PaymentProcessingService {
    
    private final Counter paymentCounter = 
        GlobalOpenTelemetry.getMeter("payment-service")
            .counterBuilder("payments_processed_total")
            .setDescription("Total number of payments processed")
            .build();
    
    public void processPayment(Payment payment) {
        // Increment custom metric
        paymentCounter.add(1, Attributes.builder()
            .put("currency", payment.getCurrency())
            .put("status", "processed")
            .build());
    }
}
```

## OpenTelemetry Configuration

### OpenTelemetry Collector Configuration (`otel-collector-config.yml`)
```yaml
receivers:
  otlp:
    protocols:
      grpc:
        endpoint: 0.0.0.0:4317
      http:
        endpoint: 0.0.0.0:4318

processors:
  batch:
    timeout: 1s
    send_batch_size: 1024
    
exporters:
  otlp/grafana:
    endpoint: grafana:4317
    tls:
      insecure: true
  prometheus:
    endpoint: "0.0.0.0:8889"

service:
  pipelines:
    traces:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/grafana]
    metrics:
      receivers: [otlp]
      processors: [batch]
      exporters: [prometheus, otlp/grafana]
    logs:
      receivers: [otlp]
      processors: [batch]
      exporters: [otlp/grafana]
```

### Service Configuration (`application.yml`)
```yaml
# OpenTelemetry configuration (automatically handled in Docker Compose)
otel:
  service:
    name: payment-service
    version: 1.0.0
  exporter:
    otlp:
      endpoint: http://otel-collector:4318
  traces:
    exporter: otlp
  metrics:
    exporter: otlp
  logs:
    exporter: otlp
```

## Development with OpenTelemetry

### Docker Compose Development Setup
The recommended approach uses Docker Compose to handle all OpenTelemetry configuration automatically:

```bash
# 1. Build and start everything with full observability
docker-compose up -d

# 2. All OpenTelemetry configuration is handled automatically:
# - Java agent is downloaded and configured
# - Service names are set appropriately
# - OTLP endpoints are configured
# - Grafana dashboards are pre-configured

# 3. Develop and test with full observability
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d @sample-requests/valid-payment.json

# 4. View traces and metrics in Grafana at http://localhost:3000
```

### Manual Local Development Setup
For custom development configurations:

```bash
# 1. Download OpenTelemetry Java agent (script provided)
./download-otel-agent.sh

# 2. Start infrastructure only
docker-compose up -d consul activemq otel-collector grafana

# 3. Run service with agent
java -javaagent:./opentelemetry-javaagent.jar \
     -Dotel.service.name=payment-service \
     -Dotel.exporter.otlp.endpoint=http://localhost:14318 \
     -jar payment-processing-system/target/payment-processing-system-1.0-SNAPSHOT.jar
```

### IDE Integration
Add JVM arguments to your IDE run configuration:
```
-javaagent:path/to/opentelemetry-javaagent.jar
-Dotel.service.name=payment-service
-Dotel.exporter.otlp.endpoint=http://localhost:14318
```

## OpenTelemetry Benefits Achieved

### Complete Observability
- **Traces**: End-to-end request flow visualization
- **Metrics**: Application and infrastructure monitoring
- **Logs**: Structured logging with trace correlation

### Vendor Neutrality
- **Open standard**: Not locked to any specific vendor
- **Multiple backends**: Can export to Grafana, Jaeger, Zipkin, Prometheus, etc.
- **Future-proof**: Industry standard for observability

### Automatic Instrumentation
- **Zero code changes** for basic instrumentation
- **Framework support**: Spring Boot, HTTP clients, databases, messaging
- **Custom instrumentation** available when needed

### Production Ready
- **Performance optimized**: Minimal overhead with sampling
- **Scalable**: OpenTelemetry Collector handles high throughput
- **Reliable**: Battle-tested in production environments

## Key OpenTelemetry Advantages

1. **Industry Standard**: CNCF graduated project, widely adopted
2. **Comprehensive**: Traces, metrics, and logs in one solution
3. **Auto-instrumentation**: Minimal code changes required
4. **Vendor Agnostic**: Works with multiple observability backends
5. **High Performance**: Optimized for production workloads
6. **Rich Ecosystem**: Extensive instrumentation libraries
7. **Future Proof**: Continuous innovation and improvement
8. **Docker Compose Integration**: Seamless local development and testing

This OpenTelemetry integration provides enterprise-grade observability with complete end-to-end traceability, making it ideal for production microservices environments and demonstrating modern observability best practices.

## Next Steps

1. **Alerting**: Set up Prometheus alerting rules in Grafana
2. **Dashboards**: Create custom Grafana dashboards for business metrics
3. **Log Analysis**: Utilize Grafana Loki for advanced log analysis
4. **Performance Monitoring**: Set up SLOs and error rate monitoring
5. **Chaos Engineering**: Use traces to analyze failure scenarios
6. **Production Deployment**: Extend Docker Compose setup for production environments
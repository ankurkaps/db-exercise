#!/bin/bash

# Download OpenTelemetry Java Agent
echo "Downloading OpenTelemetry Java Agent..."

# Create agents directory
mkdir -p agents

# Download the latest OpenTelemetry Java agent
AGENT_VERSION="v2.0.0"
AGENT_URL="https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/${AGENT_VERSION}/opentelemetry-javaagent.jar"

echo "Downloading OpenTelemetry Java Agent version ${AGENT_VERSION}..."
curl -L -o agents/opentelemetry-javaagent.jar "${AGENT_URL}"

if [ $? -eq 0 ]; then
    echo "✅ OpenTelemetry Java Agent downloaded successfully to agents/opentelemetry-javaagent.jar"
    echo ""
    echo "Usage examples:"
    echo "==============="
    echo ""
    echo "# Payment Service"
    echo "java -javaagent:agents/opentelemetry-javaagent.jar \\"
    echo "     -Dotel.service.name=payment-service \\"
    echo "     -Dotel.exporter.otlp.endpoint=http://localhost:4317 \\"
    echo "     -jar payment-service/target/payment-service-1.0.0.jar"
    echo ""
    echo "# Broker Service"  
    echo "java -javaagent:agents/opentelemetry-javaagent.jar \\"
    echo "     -Dotel.service.name=broker-service \\"
    echo "     -Dotel.exporter.otlp.endpoint=http://localhost:4317 \\"
    echo "     -jar broker-service/target/broker-service-1.0.0.jar"
    echo ""
    echo "# Fraud Check Service"
    echo "java -javaagent:agents/opentelemetry-javaagent.jar \\"
    echo "     -Dotel.service.name=fraud-check-service \\"
    echo "     -Dotel.exporter.otlp.endpoint=http://localhost:4317 \\"
    echo "     -jar fraud-check-service/target/fraud-check-service-1.0.0.jar"
else
    echo "❌ Failed to download OpenTelemetry Java Agent"
    exit 1
fi
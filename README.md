# Payment System Fraud Check Demo

A microservices payment processing system with fraud detection using Spring Boot, Apache Camel, and OpenTelemetry observability.

## Architecture Overview

This system implements a microservices architecture with three core services:

1. **Payment Processing System (PPS)** - Port 8081
   - REST API for payment submission and status tracking
   - Input validation with error handling
   - Supports both JMS and REST communication patterns

2. **Broker System (BS)** - Port 8082
   - Message broker and format transformation service
   - Handles JSON ↔ XML conversion between services
   - Implements routing and mediation patterns

3. **Fraud Check System (FCS)** - Port 8083
   - Fraud detection engine with configurable blacklist rules
   - XML-based communication for enterprise integration
   - Supports multiple fraud detection criteria

## Features

- Dual communication patterns: JSON over JMS messaging and REST APIs
- Message transformation: JSON to XML conversion between services
- ISO standard validation for countries, currencies, and formats
- Fraud detection using configurable blacklists
- OpenTelemetry distributed tracing
- Consul service discovery
- Docker Compose deployment
- Swagger UI/OpenAPI for REST endpoints

## Technology Stack

- **Runtime**: Java 21, Spring Boot 3.5.5, Spring Cloud 2025.0.0
- **Integration**: Apache Camel 4.14.0 for routing and mediation
- **Messaging**: Apache ActiveMQ Artemis for reliable messaging
- **Serialization**: Jackson (JSON), JAXB (XML) with custom adapters
- **Service Discovery**: HashiCorp Consul
- **Observability**: OpenTelemetry, Grafana LGTM Stack
- **Testing**: JUnit 5, Spring Boot Test
- **Build**: Maven multi-module project

## Project Structure

```
payment-system-fraud-check-demo/
├── shared-commons/              # Common models, validation, JAXB adapters
├── payment-processing-system/   # Payment REST API and orchestration
├── broker-system/               # Message broker and transformation
├── fraud-check-system/          # Fraud detection engine
├── sample-requests/             # Test payloads and HTTP requests
├── docker-compose.yml           # Infrastructure and services setup
├── otel-collector-config.yml    # OpenTelemetry configuration
└── pom.xml                      # Parent POM with dependency management
```

## Prerequisites

- **Java 21** or higher
- **Maven 3.8+** for building
- **Docker & Docker Compose** for containerized deployment
- **curl** or similar HTTP client for testing

## Quick Start

### Recommended: Docker Compose Setup

Docker Compose starts all services and infrastructure automatically:

*** Important *** Configure the environment variables 

1. **Clone and setup environment**:
```bash
git clone <repository-url>
cd payment-system-fraud-check-demo
cp .env.example .env  # Configure environment variables if needed

# Setup the env
source .env

# Check env are setup
env
```

2. **Start all services with Docker Compose**:
```bash

# Build and Run in detached mode
docker-compose up --build -d

# Start everything in detached mode
docker-compose up -d

# Or start with logs visible
docker-compose up

```


This starts:
- All 3 microservices: Payment Processing, Broker, and Fraud Check systems
- ActiveMQ Artemis: Message broker for JMS communication
- Consul: Service discovery and configuration
- Grafana LGTM Stack: Observability (metrics, logs, traces)
- OpenTelemetry Collector: Telemetry data collection

3. **Verify services are running**:
```bash
# Check service health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health

# Check service discovery
curl http://localhost:8500/v1/agent/services
```

4. **Access the system**:
- **Swagger UI**: http://localhost:8081/swagger-ui.html (Interactive API testing)
- **Grafana**: http://localhost:3000 (Monitoring dashboards)
- **ActiveMQ Console**: http://localhost:8161 (Message broker management)
- **Consul UI**: http://localhost:8500 (Service discovery)

5. **Manage the Docker Compose stack**:
```bash
# View running services
docker-compose ps

# View logs for all services
docker-compose logs -f

# View logs for specific service
docker-compose logs -f payment-processing-system

# Stop all services
docker-compose down

# Stop and remove volumes (clean slate)
docker-compose down -v

# Restart specific service
docker-compose restart broker-system
```

### Alternative: Local Development Mode

For developers who want to run services individually or debug locally:

1. **Build the project**:
```bash
mvn clean install
```

2. **Start infrastructure services**:
```bash
docker-compose up -d activemq consul grafana otel-collector
```

3. **Run services locally** (in separate terminals):
```bash
# Terminal 1: Payment Processing System
cd payment-processing-system && mvn spring-boot:run

# Terminal 2: Broker System
cd broker-system && mvn spring-boot:run

# Terminal 3: Fraud Check System
cd fraud-check-system && mvn spring-boot:run
```

## API Usage

### Interactive API Testing

OpenAPI/Swagger UI available at http://localhost:8081/swagger-ui.html:
- Interactive REST endpoint testing
- Request/response schema documentation
- Built-in payload validation
- Try-it-now functionality

### Submit Payment for Processing

**Valid Payment (REST)**:
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d @sample-requests/valid-payment.json
```

**Valid Payment (JMS)**:
```bash
curl -X POST http://localhost:8081/api/v1/payments/jms \
  -H "Content-Type: application/json" \
  -d @sample-requests/valid-payment.json
```

### Test Fraud Detection

**Blacklisted Name**:
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d @sample-requests/blacklisted-name.json
```

**Blacklisted Country**:
```bash
curl -X POST http://localhost:8081/api/v1/payments \
  -H "Content-Type: application/json" \
  -d @sample-requests/blacklisted-country.json
```

### Query Payment Status

```bash
# Get specific payment by transaction ID
curl http://localhost:8081/api/v1/payments/{transactionId}

# Get all payments
curl http://localhost:8081/api/v1/payments

# Filter by status
curl "http://localhost:8081/api/v1/payments?status=COMPLETED"
```

## Fraud Detection Rules

The system implements configurable fraud detection based on blacklists:

| Category | Blacklisted Values |
|----------|-------------------|
| **Names** | "Mark Imaginary", "Govind Real", "Shakil Maybe", "Chang Imagine" |
| **Countries** | CUB, IRQ, IRN, PRK, SDN, SYR |
| **Banks** | "BANK OF KUNLUN", "KARAMAY CITY COMMERCIAL BANK" |
| **Payment Instructions** | "Artillery Procurement", "Lethal Chemicals payment" |

## Payment Data Model

Payment request validation follows international standards:

| Field | Required | Type | Constraints & Validations | Example |
|-------|----------|------|---------------------------|---------|
| **transactionId** | ✅ | UUID | Must be valid UUID format, unique across system | `550e8400-e29b-41d4-a716-446655440000` |
| **payerName** | ✅ | String | Length: 1-70 characters, not blank | `John Doe` |
| **payerBank** | ✅ | String | Length: 1-70 characters, not blank | `Bank of America` |
| **payerCountryCode** | ✅ | String | ISO3166-1 Alpha-3, uppercase, 3 characters | `USA`, `GBR`, `DEU` |
| **payerAccount** | ✅ | String | Length: 8-34 characters, alphanumeric only | `1234567890123456` |
| **payeeName** | ✅ | String | Length: 1-70 characters, not blank | `Jane Smith` |
| **payeeBank** | ✅ | String | Length: 1-70 characters, not blank | `JPMorgan Chase` |
| **payeeCountryCode** | ✅ | String | ISO3166-1 Alpha-3, uppercase, 3 characters | `USA`, `GBR`, `DEU` |
| **payeeAccount** | ✅ | String | Length: 8-34 characters, alphanumeric (case insensitive) | `0987654321098765` |
| **executionDate** | ✅ | LocalDate | ISO8601 date format (YYYY-MM-DD) | `2024-12-31` |
| **amount** | ✅ | BigDecimal | Min: 0.01, max 12 digits + 2 decimal places | `1500.75` |
| **currency** | ✅ | String | ISO4217 currency code, uppercase, 3 characters | `USD`, `EUR`, `GBP` |
| **creationTimestamp** | ✅ | Instant | ISO8601 UTC timestamp (no milliseconds) | `2024-01-01T10:00:00Z` |
| **paymentInstruction** | ❌ | String | Max 140 characters, free text | `Salary payment` |

### Validation Rules Details

#### Field-Specific Validations
- UUID Format: Must follow standard UUID pattern `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`
- Country Codes: ISO 3166-1 Alpha-3 standard (e.g., USA, GBR, DEU, not US, UK, DE)
- Currency Codes: ISO 4217 standard (e.g., USD, EUR, GBP, not Dollar, Euro, Pound)
- Account Numbers: International formats including IBAN (EU), US routing numbers
- Date Formats: ISO 8601 compliance - dates as YYYY-MM-DD, timestamps as YYYY-MM-DDTHH:mm:ssZ

#### Business Rule Validations
- Amount Precision: Exactly 2 decimal places required (e.g., 100.50, not 100.5 or 100.500)
- Account Length: International account number formats (IBAN: 15-34, US: 8-17, etc.)
- Name Constraints: SWIFT/SEPA standards for international transfers
- Instruction Limits: 140 character limit aligns with SWIFT MT and ISO 20022 standards

#### Format Error Examples
| Invalid Input | Validation Error | Correct Format |
|---------------|------------------|----------------|
| `2024-01-01T10:00:00.000Z` (for executionDate) | Date field cannot contain time | `2024-01-01` |
| `2024-01-01T10:00:00.123Z` (for timestamp) | Milliseconds not supported | `2024-01-01T10:00:00Z` |
| `invalid-uuid` | Invalid UUID format | `550e8400-e29b-41d4-a716-446655440000` |
| `US` (country code) | Must be ISO Alpha-3 | `USA` |
| `Dollar` (currency) | Must be ISO 4217 code | `USD` |
| `123456` (account) | Too short, min 8 characters | `12345678` |
| `100.5` (amount) | Must have exactly 2 decimals | `100.50` |

## Monitoring and Observability

### Access Dashboards

- **OpenAPI/Swagger UI**: http://localhost:8081/swagger-ui.html (Interactive API testing)
- **Grafana Dashboard**: http://localhost:3000 (Default: admin/admin)
- **ActiveMQ Console**: http://localhost:8161 (Default: admin/admin)
- **Consul UI**: http://localhost:8500

### OpenTelemetry Integration

OpenTelemetry provides:
- Distributed Tracing: End-to-end request tracing across all services
- Metrics Collection: Application and infrastructure metrics
- Log Correlation: Structured logging with trace correlation
- Service Maps: Visual representation of service dependencies

## Communication Patterns

### Solution 1: JMS Messaging
```
Payment System <--JMS (JSON) --> Broker System <--JMS (XML) --> Fraud Check System
```

### Solution 2: REST APIs
```
Payment System <--REST (JSON) --> Broker System <--JMS (XML)--> Fraud Check System
```

Both solutions use JSON between Payment System ↔ Broker System and XML over JMS for Broker System ↔ Fraud Check System communication

## Development and Testing

### Run 
```bash
# Build
mvn clean package

# Run all tests
mvn test

# Run tests for specific module
mvn test -pl payment-processing-system
```

## Error Handling

Error handling provides detailed validation messages:

- Validation Errors: Field-specific validation with actionable guidance
- Format Errors: Clear messages for date, timestamp, and UUID format issues
- Business Logic Errors: Error codes for duplicate payments, not found, etc.
- System Errors: Generic error handling for unexpected issues

See `README-validation-error-examples.md` for detailed error response examples.

## Configuration

### Environment Variables

Key configuration options (configure in `.env` file):
```bash
# ActiveMQ Configuration
ARTEMIS_USER=<your_username>     # Default: admin
ARTEMIS_PASSWORD=<your_password> # Default: admin

# Grafana Configuration
GRAFANA_USER=<your_username>     # Default: admin
GRAFANA_PASSWORD=<your_password> # Default: admin

# Optional: Grafana Cloud Integration
GRAFANA_CLOUD_USER=<your_grafana_cloud_user>
GRAFANA_CLOUD_PASSWORD=<your_grafana_cloud_token>
```

### Profiles

- default: Local development with external dependencies
- docker: Containerized deployment configuration
- test: Test-specific configuration with embedded components

## Troubleshooting

### Common Issues

1. Port Conflicts: Ensure ports 8081-8083, 8161, 8500, 3000 are available
2. Service Discovery: Verify Consul is running and services are registered
3. Message Broker: Check ActiveMQ status via web console
4. Tracing: Ensure OpenTelemetry collector is receiving traces

### Health Checks
```bash
# Check all service health endpoints
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
curl http://localhost:8083/actuator/health
```

### Log Analysis
```bash
# View service logs
docker-compose logs -f payment-processing-system
docker-compose logs -f broker-system
docker-compose logs -f fraud-check-system
```

## Additional Documentation

- `README-OpenTelemetry.md`: Detailed OpenTelemetry setup and configuration
- `README-validation-error-examples.md`: API error response examples
- `Coding_Exercise_SolArch.md`: Original requirements specification

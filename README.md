# Kafka Ordering System

- **Reg No**   - EG/2020/3905
- **Name**     - Dissanayaka D.M.S.C.
- **Language** - Java 21 with Maven

---

## Assignment Overview

This project implements a **complete Kafka-based order processing system** that fulfills all assignment requirements. The system demonstrates real-time message processing with Avro serialization, intelligent retry logic, and dead letter queue handling for failed messages.

---

## System Architecture

```
┌─────────────┐      HTTP POST       ┌──────────────────┐
│   Client    │ ──────────────────►  │ OrderApiServer   │
│ (Postman)   │                      │   (Port 8080)    │
└─────────────┘                      └──────────────────┘
                                              │
                                              │ Validates orderId
                                              ▼
                               ┌──────────────────────────┐
                               │   OrderValidator         │
                               │  • Negative → DLQ        │
                               │  • Decimal → Retry       │
                               │  • Valid Int → Normal    │
                               └──────────────────────────┘
                                              │
                ┌─────────────────────────────┼─────────────────────────────┐
                │                             │                             │
                ▼                             ▼                             ▼
        ┌──────────────┐            ┌──────────────┐            ┌──────────────┐
        │ orders-dlq   │            │ orders-retry │            │   orders     │
        │  (Topic)     │            │  (Topic)     │            │   (Topic)    │
        └──────────────┘            └──────────────┘            └──────┬───────┘
         (Permanent               (Temporary                           │
          Failures)                Failures)                           │
                                                                        │
                                                              ┌─────────┴─────────┐
                                                              │                   │
                                                              ▼                   │
                                                    ┌──────────────────┐          │
                                                    │  OrderConsumer   │◄─────────┘
                                                    │  • Process order │
                                                    │  • Call aggreg.  │
                                                    └────────┬─────────┘
                                                             │
                                                             ▼
                                                    ┌──────────────────┐
                                                    │ PriceAggregator  │
                                                    │ • Running Avg    │
                                                    │ • Total Revenue  │
                                                    │ • Order Count    │
                                                    │ • Display Stats  │
                                                    └──────────────────┘
```

### Component Descriptions

1. **OrderApiServer** - Jetty-based REST API endpoint accepting order submissions via HTTP POST on port 8080
2. **OrderValidator** - Validates orderId and returns ValidationResult enum (VALID, RETRY, DLQ, INVALID)
3. **OrderProducer** - Publishes messages to appropriate Kafka topics with Avro serialization and callback handlers
4. **OrderConsumer** - Consumes messages from `orders` topic and processes valid orders
5. **PriceAggregator** - Thread-safe real-time aggregation using AtomicInteger and AtomicReference
6. **Kafka Topics:**
   - `orders` - Valid orders for normal processing
   - `orders-retry` - Temporary failures (decimal orderIds)
   - `orders-dlq` - Permanent failures (negative orderIds)

---

## Implementation Details

### 1. Kafka Producer & Consumer

**Producer (`OrderProducer.java`):**
- Accepts orders via REST API (HTTP POST to port 8080)
- Uses Avro serialization for all messages
- Integrates with Schema Registry for schema validation
- Configurable with acknowledgment mode (`acks=all`) and retries

**Consumer (`OrderConsumer.java`):**
- Consumes messages from `orders` topic in real-time
- Processes each order and updates aggregation statistics via `PriceAggregator`
- Uses `KafkaAvroDeserializer` with `specific.avro.reader=true`
- Displays processed orders in console

### 2. Avro Serialization

- **Schema Registry:** Running on port 8081
- **Serializer:** `KafkaAvroSerializer` for producer
- **Deserializer:** `KafkaAvroDeserializer` for consumer
- **Schema Evolution:** Supported through Schema Registry
- **Generated Classes:** Avro Maven plugin generates Java classes from schema

### 3️. Real-time Aggregation (Running Average)

**Implementation:** `PriceAggregator.java`

Calculates and displays after each order:
- **Running Average Price** - Average of all processed orders
- **Total Orders Processed** - Count of successfully consumed orders
- **Total Revenue** - Sum of all order prices
- **Current Order Price** - Price of the most recent order

**Thread-Safety:** Uses `AtomicInteger` and `AtomicReference` for concurrent updates

**Console Output Example:**
```
✓ Processed: Order{orderId=123, product=Laptop, price=1200.5}
------------------------------------------------
        REAL-TIME PRICE AGGREGATION                      
------------------------------------------------
Current Order Price:  $1200.50
Total Orders Processed: 5
Total Revenue:        $2450.99
Running Average Price: $490.20
---------------------------------------------------
```

### 4. Retry Logic for Temporary Failures

**Producer-Level Routing:**
- **Decimal orderId** (e.g., "1.5") → Routes to `orders-retry` topic immediately
- **Negative orderId** (e.g., "-5") → Routes to `orders-dlq` topic immediately
- **Invalid format** (e.g., "abc", "fail") → Rejected with HTTP 400 error


**Validation Logic (`OrderValidator.java`):**
```java
ValidationResult:
- VALID:   Non-negative integer (e.g., "123", "0", "1001")
- RETRY:   Contains decimal point (e.g., "1.5", "10.5")
- DLQ:     Negative integer (e.g., "-1", "-100")
- INVALID: Null, empty, or non-numeric (e.g., "", "abc", "fail")
```

**Producer Console Output:**
```
Temporary falier in order Order{orderId=1.5, product=Mouse, price=100.0}
RETRYING............
Produced to orders-retry: Order{orderId=1.5, product=Mouse, price=100.0}
```

### 5. Dead Letter Queue (DLQ)

**Permanent Failure Criteria:**
- **Negative orderId** (e.g., "-5") → Immediately sent to DLQ

**DLQ Topic:** `orders-dlq`

**Console Output:**
```
PERMANENT FAILURE in order :Order{orderId=-5, product=Keyboard, price=50.0}
Sending order to Dead Letter Queue (DLQ) for manual inspection...
Produced to orders-dlq: Order{orderId=-5, product=Keyboard, price=50.0}
```

---

##  How the System Works

### Message Flow

1. **HTTP Request Arrives** → `OrderApiServer` receives POST request on `/order`
2. **JSON Parsing** → Extract orderId, product, and price from request body
3. **Validation** → `OrderValidator.validateOrderId(orderId)` returns enum result
4. **Routing Decision** → `OrderApiServer` uses switch statement:
   - `VALID` → Send to `orders` topic
   - `RETRY` → Send to `orders-retry` topic
   - `DLQ` → Send to `orders-dlq` topic
   - `INVALID` → Return HTTP 400 error
5. **Producer Sends** → `OrderProducer.sendToTopic()` with Avro serialization
6. **Consumer Receives** → `OrderConsumer` polls from `orders` topic
7. **Processing** → Process order normally
8. **Aggregation** → `PriceAggregator.addPrice()` updates running statistics
9. **Display** → Console shows aggregation table

### Thread Model

- **Main Thread**: Starts `OrderApiServer` (Jetty server on port 8080)
- **Consumer Thread**: Created by `MainApp`, runs `OrderConsumer.main()`
- Both threads run concurrently for real-time processing

### Error Handling

**Producer-Level:**
- Callback handler logs success or error for each sent message
- Configured with `acks=all` for all broker acknowledgments
- Automatic retries configured (3 attempts)

**Consumer-Level:**
- Try-catch block around order processing
- Errors logged to console with exception message
- Continuous polling for new messages

---

## Quick Start Guide

### Prerequisites

- **Java:** 21 or higher
- **Maven:** 3.6+
- **Docker & Docker Compose:** Latest version
- **Git:** For version control
- **Postman or curl:** For sending test orders

### Step 1: Start Kafka Infrastructure

```bash
docker-compose up -d
```

This starts:
- **Zookeeper** - Port 2181
- **Kafka Broker** - Port 9092
- **Schema Registry** - Port 8081

**Verify containers are running:**
```bash
docker ps
```

### Step 2: Build the Project

```bash
mvn clean package
```

This will:
- Generate Java classes from Avro schema
- Compile all source code
- Package into JAR file

### Step 3: Run the Application

**Option A: Using Maven**
```bash
mvn exec:java -Dexec.mainClass="com.kafka.assignment.MainApp"
```

**Option B: Using IntelliJ IDEA**
1. Open `MainApp.java`
2. Click the green Run button
3. Application starts with both Producer (API) and Consumer running

**Expected Console Output:**
```
API server running → http://localhost:8080/order
System ready.
```

### Step 4: Send Test Orders

**Valid Order (Normal Processing):**
```bash
curl -X POST http://localhost:8080/order ^
  -H "Content-Type: application/json" ^
  -d "{\"orderId\":\"1001\",\"product\":\"Laptop\",\"price\":1200.50}"
```

**Retry Order (Decimal orderId):**
```bash
curl -X POST http://localhost:8080/order ^
  -H "Content-Type: application/json" ^
  -d "{\"orderId\":\"1.5\",\"product\":\"Mouse\",\"price\":25.99}"
```

**DLQ Order (Negative orderId):**
```bash
curl -X POST http://localhost:8080/order ^
  -H "Content-Type: application/json" ^
  -d "{\"orderId\":\"-5\",\"product\":\"Keyboard\",\"price\":75.00}"
```

---

## Project Structure

```
kafka-order-system/
├── docker-compose.yml                  # Kafka infrastructure setup
├── pom.xml                             # Maven dependencies
├── README.md                           # This documentation
│
├── src/
│   └── main/
│       ├── avro/
│       │   └── order.avsc              # Avro schema definition (Assignment requirement)
│       │
│       ├── java/com/kafka/assignment/
│       │   ├── Config.java             # Configuration loader
│       │   ├── MainApp.java            # Main entry point (runs producer + consumer)
│       │   ├── Order.java              # Generated Avro class
│       │   ├── OrderApiServer.java     # REST API Producer (Port 8080)
│       │   ├── OrderProducer.java      # Kafka producer with Avro
│       │   ├── OrderConsumer.java      # Kafka consumer
│       │   ├── OrderValidator.java     # Message validation & routing logic
│       │   └── PriceAggregator.java    # Real-time aggregation (Running average)
│       │
│       └── resources/
│           ├── config.properties       # Kafka & topic configuration
│           └── logback.xml             # Logging configuration
│
└── target/
    └── kafka-order-system-1.0-SNAPSHOT.jar
```
---

## Technology Stack

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Language** | Java | 21 | Core programming language |
| **Build Tool** | Maven | 3.9+ | Dependency management & build |
| **Message Broker** | Apache Kafka | 3.5.1 | Message streaming platform |
| **Serialization** | Apache Avro | 1.11.1 | Schema-based serialization |
| **Schema Registry** | Confluent | 7.6.0 | Avro schema management |
| **REST API** | Jetty Server | 11.0.15 | HTTP endpoint for orders |
| **Logging** | Logback | 1.2.12 | Application logging |
| **Containerization** | Docker | Latest | Kafka infrastructure |

---



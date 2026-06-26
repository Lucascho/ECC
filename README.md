# E-Commerce Campaign Touch Task System

Spring Boot MVP backend for managing e-commerce marketing campaigns, touch tasks, mock message delivery, member in-app messages, click tracking, and campaign analytics.

This repository follows the product and engineering boundaries in `docs_AI_READY_PRD_V1_MVP.md` and `AGENTS.md`.

## Project Summary

The system lets an admin user:

- Create and manage marketing campaigns.
- Create touch tasks for a campaign with audience rules and delivery channels.
- Execute a touch task manually.
- Simulate member lookup through a mock member profile client.
- Simulate `IN_APP`, `EMAIL`, and `PUSH` delivery through mock providers.
- Create member messages only for `IN_APP` deliveries.
- Track in-app message clicks.
- Query campaign analytics from `touch_delivery`.

The MVP intentionally does not implement the existing e-commerce platform domains such as member management, products, orders, coupons, payments, carts, or inventory.

## Tech Stack

- Java 17
- Spring Boot 3.x
- Spring Web
- Spring Data JPA
- Spring Validation
- PostgreSQL
- Flyway
- Springdoc OpenAPI Swagger UI
- Docker Compose
- JUnit 5 / Spring Boot Test
- Maven

## MVP Scope

Implemented backend capabilities:

- Campaign management
- Touch Task management
- Audience Rule validation
- Mock `MemberProfileClient`
- Mock `TouchProvider`
- `TouchDelivery`
- `MemberMessage`
- `CampaignEvent`
- `CampaignAnalytics`
- Swagger UI
- Flyway migration
- Docker Compose PostgreSQL
- JUnit tests

## Out Of Scope

This MVP must not include:

- Member table or member CRUD
- Product table or product CRUD
- Order table or order creation
- Coupon table or coupon claim flow
- Payment flow
- Cart table
- Inventory table
- Real Email provider
- Real Push provider
- Login system
- Spring Security, JWT, or OAuth2
- Redis, RabbitMQ, Kafka, or Elasticsearch
- Vue, React, or any frontend

Mock member data stays in:

```text
backend/src/main/resources/mock/members.json
```

## Allowed Database Tables

Only these business tables are allowed:

- `campaign`
- `touch_task`
- `touch_delivery`
- `member_message`
- `campaign_event`

Schema is managed by Flyway:

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
```

## Setup Instructions

Prerequisites:

- Java 17
- Docker Desktop or Docker Engine
- Maven, or use the included Maven wrapper

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run tests:

```bash
cd backend
./mvnw test
```

Run the backend:

```bash
cd backend
./mvnw spring-boot:run
```

The application runs on:

```text
http://localhost:8080
```

## Docker Compose Instructions

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Check container status:

```bash
docker compose ps
```

Stop PostgreSQL:

```bash
docker compose down
```

The PostgreSQL service uses:

```text
Database: campaign_touch
Username: campaign_touch
Password: campaign_touch
Port: 5432
```

## Swagger URL

After starting the backend, open:

```text
http://localhost:8080/swagger-ui.html
```

## Demo Curl Script

The API uses headers to simulate identity:

- Admin APIs require `X-Admin-User`.
- Member APIs require `X-Member-Id`.

Example end-to-end demo:

```bash
BASE_URL="http://localhost:8080"
ADMIN_USER="admin-demo"
MEMBER_ID="M001"

CAMPAIGN_ID=$(
  curl -s -X POST "$BASE_URL/api/admin/campaigns" \
    -H "Content-Type: application/json" \
    -H "X-Admin-User: $ADMIN_USER" \
    -d '{
      "name": "Summer Promo",
      "type": "PROMOTION",
      "description": "MVP demo campaign",
      "landingPageUrl": "https://example.com/summer",
      "startTime": "2026-06-26T00:00:00",
      "endTime": "2026-07-31T23:59:59"
    }' | jq -r '.id'
)

curl -s -X POST "$BASE_URL/api/admin/campaigns/$CAMPAIGN_ID/activate" \
  -H "X-Admin-User: $ADMIN_USER"

TASK_ID=$(
  curl -s -X POST "$BASE_URL/api/admin/campaigns/$CAMPAIGN_ID/touch-tasks" \
    -H "Content-Type: application/json" \
    -H "X-Admin-User: $ADMIN_USER" \
    -d '{
      "taskName": "VIP multi-channel touch",
      "audienceRule": {
        "memberLevels": ["VIP", "GOLD"],
        "lastLoginDaysLessThan": 30,
        "favoriteCategories": ["3C"],
        "hasCartItems": true
      },
      "channels": ["IN_APP", "EMAIL", "PUSH"],
      "messageTitle": "Summer deal is live",
      "messageContent": "Open the campaign page to see the offer."
    }' | jq -r '.id'
)

curl -s -X POST "$BASE_URL/api/admin/touch-tasks/$TASK_ID/execute" \
  -H "X-Admin-User: $ADMIN_USER"

curl -s "$BASE_URL/api/member/messages?campaignId=$CAMPAIGN_ID" \
  -H "X-Member-Id: $MEMBER_ID"

MESSAGE_ID=$(
  curl -s "$BASE_URL/api/member/messages?campaignId=$CAMPAIGN_ID" \
    -H "X-Member-Id: $MEMBER_ID" | jq -r '.[0].id'
)

curl -s -X POST "$BASE_URL/api/member/messages/$MESSAGE_ID/click" \
  -H "X-Member-Id: $MEMBER_ID"

curl -s "$BASE_URL/api/admin/campaigns/$CAMPAIGN_ID/analytics" \
  -H "X-Admin-User: $ADMIN_USER"
```

If `jq` is not installed, run the same curl commands manually and copy the returned `id` values into `CAMPAIGN_ID`, `TASK_ID`, and `MESSAGE_ID`.

## Test Command

```bash
cd backend
./mvnw test
```

Alternative:

```bash
cd backend
mvn test
```

## Reviewer Checklist

Before accepting changes, verify:

- No out-of-scope tables were added.
- Only `campaign`, `touch_task`, `touch_delivery`, `member_message`, and `campaign_event` are created.
- No member, product, order, coupon, payment, cart, or inventory tables exist.
- No Redis, RabbitMQ, Kafka, or Elasticsearch dependency was added.
- No Spring Security, JWT, or OAuth2 dependency was added.
- No Vue, React, or frontend was added.
- Mock member data stays in `backend/src/main/resources/mock/members.json`.
- Controllers do not contain business logic.
- Services handle business rules and transaction boundaries.
- Entities are not exposed directly by API responses.
- DTOs are used for request and response objects.
- Flyway creates the schema.
- `spring.jpa.hibernate.ddl-auto=validate` is configured.
- Analytics use `touch_delivery` as the primary source of truth.
- `campaign_event` remains an event log, not the main analytics source.
- `./mvnw test` passes.
- Application startup succeeds with PostgreSQL running.
- Swagger is available at `http://localhost:8080/swagger-ui.html`.

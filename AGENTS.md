# AGENTS.md

## Project

E-Commerce Campaign Touch Task System

This project implements the MVP backend service defined in:

```text
docs/AI_READY_PRD_V1_MVP.md
```

All implementation must follow the PRD strictly.

---

## Tech Stack

Use:

```text
Java 17
Spring Boot 3.x
Spring Web
Spring Data JPA
Spring Validation
PostgreSQL
Flyway
Swagger UI / OpenAPI
Docker Compose
JUnit 5
```

Do not use:

```text
Redis
RabbitMQ
Kafka
Spring Security
JWT
OAuth2
Vue
React
Elasticsearch
```

---

## MVP Scope

Implement only:

```text
Campaign management
Touch Task management
Audience Rule validation
Mock MemberProfileClient
Mock TouchProvider
TouchDelivery
MemberMessage
CampaignEvent
CampaignAnalytics
Swagger UI
Flyway migration
Docker Compose PostgreSQL
JUnit tests
```

---

## Hard Boundaries

Do not implement:

```text
member table
product table
order table
coupon table
payment table
cart table
inventory table
member CRUD
product CRUD
order creation
coupon claim
payment flow
real Email provider
real Push provider
frontend
login system
```

Mock member data must stay in:

```text
src/main/resources/mock/members.json
```

Do not import mock members into a database table.

---

## Allowed Database Tables

Only create:

```text
campaign
touch_task
touch_delivery
member_message
campaign_event
```

Do not create any other business tables.

---

## Package Root

Use:

```text
com.example.campaigntouch
```

Recommended packages:

```text
common
campaign
touch
message
analytics
external
config
```

---

## Implementation Rules

1. Do not put business logic in controllers.
2. Controllers only handle request mapping, headers, DTO input and DTO output.
3. Services handle business rules and transaction boundaries.
4. Repositories only handle database access.
5. Do not expose Entity objects directly in API responses.
6. Use DTOs for request and response.
7. Use enums for statuses, types and channels.
8. Use Flyway for schema creation.
9. Use `spring.jpa.hibernate.ddl-auto=validate`.
10. Do not use Hibernate `create` or `update`.
11. Analytics must use `touch_delivery` as the primary source of truth.
12. `campaign_event` is an event log, not the main analytics source.

---

## Core Business Rules

1. Campaign status values: `DRAFT`, `ACTIVE`, `PAUSED`, `ENDED`.
2. Touch Task status values: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`.
3. Touch Delivery status values: `PENDING`, `SENT`, `FAILED`, `CLICKED`.
4. Touch Channels: `IN_APP`, `EMAIL`, `PUSH`.
5. Only `ACTIVE` Campaign can execute Touch Task.
6. Only `PENDING` Touch Task can be executed.
7. Completed Touch Task cannot be executed again.
8. `EMAIL` and `PUSH` must not create `member_message`.
9. Only `IN_APP` creates `member_message`.
10. Only `IN_APP` supports click tracking.
11. Repeated click on the same message must not create duplicated `CLICK` event.
12. `ENDED` Campaign cannot be activated again.

---

## Error Response Format

Use:

```json
{
  "code": "ERROR_CODE",
  "message": "Human readable message."
}
```

Required error codes include:

```text
VALIDATION_ERROR
ADMIN_USER_REQUIRED
MEMBER_ID_REQUIRED
CAMPAIGN_NOT_FOUND
TOUCH_TASK_NOT_FOUND
MESSAGE_NOT_FOUND
INVALID_CAMPAIGN_STATUS
INVALID_TOUCH_TASK_STATUS
INVALID_TOUCH_DELIVERY_STATUS
INVALID_AUDIENCE_RULE
UNSUPPORTED_TOUCH_CHANNEL
MESSAGE_NOT_OWNED_BY_MEMBER
DUPLICATE_DELIVERY
INTERNAL_ERROR
```

---

## Test Command

Run tests with:

```bash
cd backend
./mvnw test
```

or:

```bash
cd backend
mvn test
```

---

## Run Command

Start PostgreSQL:

```bash
docker compose up -d postgres
```

Run backend:

```bash
cd backend
./mvnw spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

---

## Review Checklist

Before finishing any task, verify:

```text
No out-of-scope tables were added.
No Redis / RabbitMQ / Kafka was added.
No Spring Security / JWT / OAuth2 was added.
No frontend was added.
Tests pass.
Application starts.
Swagger is available.
The implemented behavior matches docs/AI_READY_PRD_V1_MVP.md.
```

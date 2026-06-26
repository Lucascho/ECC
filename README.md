# 電商活動觸達任務系統

繁體中文 | [English](#e-commerce-campaign-touch-task-system)

Spring Boot MVP 後端服務，用於管理電商行銷活動、觸達任務、Mock 訊息派送、會員站內信、點擊追蹤與活動成效統計。

本專案嚴格遵守 `docs_AI_READY_PRD_V1_MVP.md` 與 `AGENTS.md` 定義的產品範圍與工程邊界。

## 專案摘要

系統提供管理者完成以下流程：

- 建立與管理行銷活動。
- 在活動底下建立觸達任務，設定客群規則與觸達渠道。
- 手動執行觸達任務。
- 透過 Mock Member Profile Client 模擬既有會員系統查詢目標會員。
- 透過 Mock Touch Provider 模擬 `IN_APP`、`EMAIL`、`PUSH` 派送。
- 僅針對 `IN_APP` 派送建立會員站內信。
- 追蹤會員站內信點擊。
- 以 `touch_delivery` 為主要資料來源查詢活動成效。

MVP 不實作既有電商平台的基礎領域，例如會員管理、商品、訂單、優惠券、付款、購物車或庫存。

## 技術棧

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

## MVP 範圍

目前已實作的後端能力：

- Campaign 活動管理
- Touch Task 觸達任務管理
- Audience Rule 客群規則驗證
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

## 不在本次範圍

本 MVP 不包含：

- member table 或會員 CRUD
- product table 或商品 CRUD
- order table 或訂單建立
- coupon table 或優惠券領取流程
- payment flow
- cart table
- inventory table
- 真實 Email provider
- 真實 Push provider
- 登入系統
- Spring Security、JWT 或 OAuth2
- Redis、RabbitMQ、Kafka 或 Elasticsearch
- Vue、React 或任何前端

Mock 會員資料固定放在：

```text
backend/src/main/resources/mock/members.json
```

## 允許的資料表

MVP 只允許建立以下 business tables：

- `campaign`
- `touch_task`
- `touch_delivery`
- `member_message`
- `campaign_event`

Schema 由 Flyway 管理：

```text
backend/src/main/resources/db/migration/V1__init_schema.sql
```

## 環境設定

前置需求：

- Java 17
- Docker Desktop 或 Docker Engine
- Maven，或使用專案內建 Maven wrapper

啟動 PostgreSQL：

```bash
docker compose up -d postgres
```

執行測試：

```bash
cd backend
./mvnw test
```

啟動後端：

```bash
cd backend
./mvnw spring-boot:run
```

Application URL：

```text
http://localhost:8080
```

## Docker Compose 指令

啟動 PostgreSQL：

```bash
docker compose up -d postgres
```

查看 container 狀態：

```bash
docker compose ps
```

停止 PostgreSQL：

```bash
docker compose down
```

PostgreSQL 連線設定：

```text
Database: campaign_touch
Username: campaign_touch
Password: campaign_touch
Port: 5432
```

## Swagger URL

啟動後端後開啟：

```text
http://localhost:8080/swagger-ui.html
```

## Demo Curl Script

API 使用 HTTP Header 模擬身份：

- Admin APIs 需要 `X-Admin-User`
- Member APIs 需要 `X-Member-Id`

完整 demo flow：

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

如果沒有安裝 `jq`，可以手動執行 curl，並從 response 複製 `id` 到 `CAMPAIGN_ID`、`TASK_ID`、`MESSAGE_ID`。

## 測試指令

```bash
cd backend
./mvnw test
```

替代方式：

```bash
cd backend
mvn test
```

## Reviewer Checklist

驗收前請確認：

- 沒有新增 out-of-scope tables。
- 只建立 `campaign`、`touch_task`、`touch_delivery`、`member_message`、`campaign_event`。
- 沒有 member、product、order、coupon、payment、cart、inventory tables。
- 沒有加入 Redis、RabbitMQ、Kafka 或 Elasticsearch dependency。
- 沒有加入 Spring Security、JWT 或 OAuth2 dependency。
- 沒有加入 Vue、React 或任何 frontend。
- Mock member data 保持在 `backend/src/main/resources/mock/members.json`。
- Controller 不包含 business logic。
- Service 負責 business rules 與 transaction boundaries。
- API response 不直接暴露 Entity。
- Request/Response 使用 DTO。
- Schema 由 Flyway 建立。
- `spring.jpa.hibernate.ddl-auto=validate` 已設定。
- Analytics 以 `touch_delivery` 作為主要資料來源。
- `campaign_event` 僅作為 event log，不作為主要 analytics source。
- `./mvnw test` 通過。
- PostgreSQL 啟動後 application startup 成功。
- Swagger 可在 `http://localhost:8080/swagger-ui.html` 開啟。

---

# E-Commerce Campaign Touch Task System

[繁體中文](#電商活動觸達任務系統) | English

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

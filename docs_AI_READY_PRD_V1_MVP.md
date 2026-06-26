# **電商活動觸達任務系統**

# **AI-Ready PRD 與技術規格書**

**English Name:** E-Commerce Campaign Touch Task System \- AI-Ready PRD & Technical Specification  
**Version:** v1.0-MVP  
**Status:** Draft for Review  
**Owner:** Ryan Chen  
**Target Reader:** Reviewer, AI Coding Assistant, Backend Developer  
**Last Updated:** 2026-06-26

---

# **1\. Executive Summary**

本文件定義「電商活動觸達任務系統」MVP 階段的產品需求、系統邊界、資料模型、API 規格、Service Flow、技術架構、AI 實作限制與 Demo Flow。

本系統聚焦於既有電商平台中的「行銷活動觸達」流程，讓營運人員可以建立活動、設定觸達任務、定義客群條件與觸達渠道，並透過 Mock Client / Mock Provider 模擬既有會員系統與外部觸達服務。

系統會產生觸達紀錄、站內信、點擊事件與活動成效統計。

本 MVP 不實作會員、商品、訂單、優惠券、付款、物流等既有電商基礎系統。

---

# **2\. Document Purpose**

本文件目的如下：

1. 定義 MVP 階段的產品範圍與技術邊界。  
2. 明確說明本系統負責與不負責的功能。  
3. 定義核心名詞，避免 Campaign、Touch Task、Touch Delivery、Member Message 等概念混淆。  
4. 定義 MVP 允許建立的資料表與禁止建立的資料表。  
5. 定義 API Contract、Service Flow、狀態流轉與錯誤規則。  
6. 作為 Entity、Repository、Service、Controller、DTO、Flyway Migration、JUnit Test 的實作依據。  
7. 讓 AI 工具可以根據本文件實作出符合邊界的 Spring Boot 後端服務。

---

# **3\. System Positioning**

## **3.1 系統名稱**

**電商活動觸達任務系統**  
**E-Commerce Campaign Touch Task System**

---

## **3.2 一句話描述**

本系統提供電商營運人員針對特定行銷活動建立觸達任務，設定客群條件、觸達渠道與訊息文案。系統會透過 Mock Member Profile Client 模擬既有會員系統取得目標會員，透過 Mock Touch Provider 模擬站內信、Email、Push 派送，並提供觸達紀錄、站內信點擊追蹤與活動成效統計。

---

# **4\. Core Design Principles**

本 MVP 遵守以下設計原則：

1. 只做本次活動觸達所需新增能力。  
2. 不重複實作既有電商基礎系統。  
3. 外部依賴以 Adapter / Mock Client 模擬。  
4. MVP 以可完成、可執行、可驗證為優先。  
5. 所有非核心功能放入 Phase 2 或 Out of Scope。  
6. 文件需讓 AI 明確知道可以實作什麼、不得實作什麼。

---

# **5\. Existing System Assumptions**

本專案假設既有電商平台已經存在以下系統：

| 既有系統 | MVP 是否實作 | MVP 處理方式 |
| ----- | ----- | ----- |
| 會員系統 Member Service | 否 | 使用 Mock Member Profile Client 模擬查詢目標會員 |
| 商品系統 Product Service | 否 | 活動只保存 landingPageUrl，不管理商品資料 |
| 訂單系統 Order Service | 否 | MVP 不處理下單與轉換 |
| 優惠券系統 Coupon Service | 否 | MVP 不發券，優惠內容僅作為文案描述 |
| 付款系統 Payment Service | 否 | 不實作 |
| 物流系統 Logistics Service | 否 | 不實作 |
| Email / Push 第三方服務 | 否 | 使用 Mock Touch Provider 模擬派送 |
| 登入與權限系統 | 否 | 使用 HTTP Header 模擬身份 |

---

# **6\. MVP Scope**

MVP 只包含以下功能。

## **6.1 Campaign 活動管理**

提供營運人員建立與管理活動基本資料。

MVP 支援：

* 建立活動  
* 查詢活動列表  
* 查詢活動詳情  
* 編輯活動  
* 啟用活動  
* 暫停活動  
* 結束活動

活動資料只代表本次行銷活動的基本設定，不包含商品庫存、優惠券、訂單或付款邏輯。

---

## **6.2 Touch Task 觸達任務管理**

提供營運人員針對某個 Campaign 建立觸達任務。

一個 Campaign 可以有多個 Touch Task。

MVP 支援：

* 建立觸達任務  
* 查詢活動底下的觸達任務列表  
* 查詢觸達任務詳情  
* 手動執行觸達任務

MVP 不支援自動排程。所有觸達任務必須透過 execute API 手動觸發。

---

## **6.3 Audience Rule 觸達客群條件**

Audience Rule 是本次活動觸達的條件設定，不是會員資料管理功能。

MVP 支援固定欄位 JSON 格式的客群條件。

| 欄位 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| memberLevels | string array | 否 | 會員等級。null 或空陣列表示不限會員等級 |
| lastLoginDaysLessThan | integer | 否 | 最近 N 天內登入。null 表示不限登入天數 |
| favoriteCategories | string array | 否 | 偏好分類。null 或空陣列表示不限分類 |
| hasCartItems | boolean | 否 | true 表示需有購物車商品；false 表示需無購物車商品；null 表示不限 |

範例：

{  
  "memberLevels": \["VIP", "GOLD"\],  
  "lastLoginDaysLessThan": 30,  
  "favoriteCategories": \["3C"\],  
  "hasCartItems": true  
}

MVP 不建立 member table。會員資料由 Mock Member Profile Client 模擬既有會員服務回傳。

---

## **6.4 Touch Channel 觸達渠道**

MVP 支援三種觸達渠道：

| Channel | 說明 | MVP 行為 |
| ----- | ----- | ----- |
| IN\_APP | 站內信 | 建立 member\_message，會員可查詢與點擊 |
| EMAIL | Email | 使用 Mock Provider 模擬派送，只記錄 SENT / FAILED |
| PUSH | App Push | 使用 Mock Provider 模擬派送，只記錄 SENT / FAILED |

MVP 不串接真實 Email、Firebase Push、LINE、SMS 或其他第三方服務。

---

## **6.5 Campaign Analytics 活動成效統計**

MVP 提供活動觸達成效統計。

| 欄位 | 說明 |
| ----- | ----- |
| campaignId | 活動 ID |
| campaignName | 活動名稱 |
| targetMemberCount | 被觸達會員數，依 memberId 去重 |
| deliveryCount | 所有渠道的 touch\_delivery 總數 |
| sentCount | SENT \+ CLICKED 的 delivery 數 |
| failedCount | FAILED 的 delivery 數 |
| inAppSentCount | IN\_APP 且狀態為 SENT 或 CLICKED 的 delivery 數 |
| clickCount | IN\_APP 且狀態為 CLICKED 的 delivery 數 |
| clickThroughRate | clickCount / inAppSentCount |

計算規則：

targetMemberCount \= count(distinct member\_id)  
deliveryCount \= count(touch\_delivery)  
sentCount \= count(status in SENT, CLICKED)  
failedCount \= count(status \= FAILED)  
inAppSentCount \= count(channel \= IN\_APP and status in SENT, CLICKED)  
clickCount \= count(channel \= IN\_APP and status \= CLICKED)  
clickThroughRate \= clickCount / inAppSentCount

若 `inAppSentCount = 0`，`clickThroughRate = 0`。

---

# **7\. Out of Scope**

以下功能不屬於 MVP，AI 實作時不得主動新增。

## **7.1 既有電商基礎系統**

MVP 不實作：

會員管理  
商品管理  
商品庫存  
訂單管理  
優惠券管理  
付款  
物流  
會員註冊  
會員登入  
會員等級計算  
購物車管理

---

## **7.2 進階行銷功能**

MVP 不實作：

優惠券發放  
訂單轉換追蹤  
GMV 統計  
A/B Test  
推薦系統  
多語系文案  
個人化推薦  
AI 文案產生  
行銷自動化流程編排

---

## **7.3 進階技術功能**

MVP 不實作：

RabbitMQ  
Kafka  
Redis  
Elasticsearch  
排程自動派送  
失敗重試  
Dead Letter Queue  
Rate Limit  
分散式鎖  
完整 Spring Security  
JWT  
OAuth2 / OIDC  
Vue 後台  
React 後台

---

# **8\. Phase 2 Scope**

以下功能可作為未來擴充，但不影響 MVP 驗收：

RabbitMQ 非同步觸達派送  
排程自動執行 Touch Task  
Redis 活動快取  
Redis 防止重複觸達  
失敗重試與 Dead Letter Queue  
真實 Email Provider 串接  
真實 Push Provider 串接  
優惠券服務串接  
訂單轉換事件串接  
AI 文案產生器  
Vue Admin 後台  
A/B Test  
多語系文案  
更完整的 Dashboard

---

# **9\. Identity and Authentication Boundary**

MVP 不實作完整登入與權限系統。

後台 API 使用 Header 模擬管理員身份：

X-Admin-User: admin

會員 API 使用 Header 模擬會員身份：

X-Member-Id: 1001

限制：

1. 若會員 API 未帶 X-Member-Id，回傳錯誤。  
2. 會員只能查詢自己的 member\_message。  
3. 會員只能點擊自己的 member\_message。  
4. 若嘗試點擊其他會員的 message，需回傳錯誤。

未來可擴充為 Spring Security \+ JWT / OAuth2。

---

# **10\. Glossary**

## **10.1 Campaign**

Campaign 代表一個行銷活動的基本資料。

範例：

618 3C 限時促銷  
雙 11 活動提醒  
會員日活動  
新品上市通知  
購物車提醒活動

Campaign 只負責描述活動本身，例如名稱、活動類型、活動時間、活動狀態與活動頁 URL。

Campaign 不負責商品管理、優惠券管理、訂單管理、會員管理、付款與物流。

一個 Campaign 可以有多個 Touch Task。

---

## **10.2 Touch Task**

Touch Task 代表某個 Campaign 底下的一次觸達任務。

範例：

活動開始當天早上 10 點推播  
活動結束前三小時站內信提醒  
新品上市 Email 通知  
購物車提醒 Push 通知

Touch Task 定義：

這次觸達屬於哪個 Campaign  
要觸達哪些目標會員  
要使用哪些觸達渠道  
要送出什麼標題與內容  
目前任務狀態是什麼  
是否已經執行

一個 Campaign 可以有多個 Touch Task。  
一個 Touch Task 執行後，會產生多筆 Touch Delivery。

MVP 中 Touch Task 不支援自動排程，只能透過 execute API 手動執行。

---

## **10.3 Audience Rule**

Audience Rule 代表本次 Touch Task 要觸達哪些會員的條件。

Audience Rule 不是會員資料，也不是會員管理功能。

MVP 使用固定欄位 JSON 格式表示 Audience Rule。

---

## **10.4 Touch Target Member**

Touch Target Member 是 Mock Member Profile Client 回傳的目標會員資料。

它代表外部會員系統查詢後的結果，不代表本系統擁有會員資料。

MVP 中 Touch Target Member 只需要包含觸達所需的最小欄位，例如：

memberId  
email  
pushToken  
memberLevel  
favoriteCategories  
lastLoginAt  
hasCartItems

MVP 不應儲存完整會員個資，也不應建立 member table。

---

## **10.5 Touch Delivery**

Touch Delivery 代表某個 Touch Task 對某個會員、某個渠道的一次觸達紀錄。

如果一個 Touch Task 有 100 位目標會員，並使用 2 個渠道，則會產生：

100 x 2 \= 200 筆 Touch Delivery

Touch Delivery 是活動成效統計的主要資料來源。

Touch Delivery 狀態包含：

PENDING  
SENT  
FAILED  
CLICKED

只有 IN\_APP channel 的 Touch Delivery 可以變成 CLICKED。

---

## **10.6 Member Message**

Member Message 代表會員可查詢與點擊的站內信訊息。

只有 IN\_APP channel 會產生 Member Message。

EMAIL / PUSH 不會產生 Member Message。

會員點擊 Member Message 後：

member\_message.clicked\_at 寫入點擊時間  
touch\_delivery.status 更新為 CLICKED  
campaign\_event 新增 CLICK event

MVP 中同一筆 Member Message 重複點擊時，不重複新增 CLICK event，也不重複計入 clickCount。

---

## **10.7 Campaign Event**

Campaign Event 代表活動觸達流程中的事件紀錄。

MVP 支援三種事件：

SENT  
FAILED  
CLICK

MVP 不支援以下事件：

IMPRESSION  
COUPON\_CLAIMED  
ORDER\_CREATED  
CONVERTED  
GMV

---

# **11\. Status Transition Rules**

## **11.1 Campaign Status**

Campaign 狀態：

DRAFT  
ACTIVE  
PAUSED  
ENDED

允許流轉：

DRAFT → ACTIVE  
ACTIVE → PAUSED  
PAUSED → ACTIVE  
ACTIVE → ENDED  
PAUSED → ENDED  
DRAFT → ENDED

不允許流轉：

ENDED → ACTIVE  
ENDED → PAUSED  
ENDED → DRAFT

行為限制：

| Campaign Status | 可編輯 | 可建立 Touch Task | 可執行 Touch Task | 可點擊站內信 |
| ----- | ----- | ----- | ----- | ----- |
| DRAFT | 是 | 是 | 否 | 否 |
| ACTIVE | 部分欄位 | 是 | 是 | 是 |
| PAUSED | 否 | 否 | 否 | 否 |
| ENDED | 否 | 否 | 否 | 否 |

MVP 規則：

只有 ACTIVE 的 Campaign 可以執行 Touch Task。  
只有 ACTIVE 的 Campaign 可以記錄 CLICK event。

---

## **11.2 Touch Task Status**

Touch Task 狀態：

PENDING  
PROCESSING  
COMPLETED  
FAILED

允許流轉：

PENDING → PROCESSING  
PROCESSING → COMPLETED  
PROCESSING → FAILED

MVP 執行規則：

1. 只有 PENDING 狀態的 Touch Task 可以 execute。  
2. PROCESSING、COMPLETED、FAILED 狀態不可再次 execute。  
3. 若需要重新觸達，必須建立新的 Touch Task。  
4. MVP 不支援 retry。

---

## **11.3 Touch Delivery Status**

Touch Delivery 狀態：

PENDING  
SENT  
FAILED  
CLICKED

允許流轉：

PENDING → SENT  
PENDING → FAILED  
SENT → CLICKED

不允許流轉：

FAILED → SENT  
FAILED → CLICKED  
CLICKED → SENT  
CLICKED → FAILED

MVP 規則：

只有 IN\_APP delivery 可以從 SENT 變成 CLICKED。  
EMAIL / PUSH delivery 不支援 CLICKED。

---

# **12\. Data Model**

MVP 僅允許建立以下 5 張資料表：

campaign  
touch\_task  
touch\_delivery  
member\_message  
campaign\_event

MVP 不得建立以下資料表：

member  
product  
order  
coupon  
payment  
cart  
inventory

---

## **12.1 Table: campaign**

用途：儲存行銷活動基本資料。

| 欄位 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| id | BIGSERIAL / BIGINT | 是 | Campaign ID，主鍵 |
| name | VARCHAR(100) | 是 | 活動名稱 |
| type | VARCHAR(50) | 是 | 活動類型 |
| description | TEXT | 否 | 活動描述 |
| landing\_page\_url | VARCHAR(500) | 否 | 既有電商平台活動頁 URL |
| start\_time | TIMESTAMP | 是 | 活動開始時間 |
| end\_time | TIMESTAMP | 是 | 活動結束時間 |
| status | VARCHAR(20) | 是 | 活動狀態 |
| created\_by | VARCHAR(100) | 是 | 建立者，來自 X-Admin-User |
| created\_at | TIMESTAMP | 是 | 建立時間 |
| updated\_at | TIMESTAMP | 是 | 更新時間 |

status 允許值：

DRAFT  
ACTIVE  
PAUSED  
ENDED

type 允許值：

PROMOTION  
NEW\_PRODUCT  
MEMBER\_RECALL  
CART\_REMINDER

---

## **12.2 Table: touch\_task**

用途：儲存某個 Campaign 底下的一次觸達任務。

| 欄位 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| id | BIGSERIAL / BIGINT | 是 | Touch Task ID，主鍵 |
| campaign\_id | BIGINT | 是 | 所屬 Campaign ID |
| task\_name | VARCHAR(100) | 是 | 觸達任務名稱 |
| audience\_rule\_json | JSONB | 是 | 觸達客群條件 |
| channels\_json | JSONB | 是 | 觸達渠道陣列 |
| message\_title | VARCHAR(200) | 是 | 觸達訊息標題 |
| message\_content | TEXT | 是 | 觸達訊息內容 |
| status | VARCHAR(20) | 是 | 觸達任務狀態 |
| executed\_at | TIMESTAMP | 否 | 任務執行時間 |
| created\_at | TIMESTAMP | 是 | 建立時間 |
| updated\_at | TIMESTAMP | 是 | 更新時間 |

audience\_rule\_json 範例：

{  
  "memberLevels": \["VIP", "GOLD"\],  
  "lastLoginDaysLessThan": 30,  
  "favoriteCategories": \["3C"\],  
  "hasCartItems": true  
}

channels\_json 範例：

\["IN\_APP", "PUSH"\]

status 允許值：

PENDING  
PROCESSING  
COMPLETED  
FAILED

---

## **12.3 Table: touch\_delivery**

用途：儲存每一筆實際觸達紀錄。

| 欄位 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| id | BIGSERIAL / BIGINT | 是 | Touch Delivery ID，主鍵 |
| touch\_task\_id | BIGINT | 是 | 所屬 Touch Task ID |
| campaign\_id | BIGINT | 是 | 所屬 Campaign ID |
| member\_id | VARCHAR(100) | 是 | 會員 ID，來自外部會員系統 |
| channel | VARCHAR(20) | 是 | 觸達渠道 |
| title | VARCHAR(200) | 是 | 派送標題 |
| content | TEXT | 是 | 派送內容 |
| status | VARCHAR(20) | 是 | 派送狀態 |
| sent\_at | TIMESTAMP | 否 | 成功派送時間 |
| clicked\_at | TIMESTAMP | 否 | 點擊時間，僅 IN\_APP 使用 |
| failed\_reason | VARCHAR(500) | 否 | 派送失敗原因 |
| created\_at | TIMESTAMP | 是 | 建立時間 |
| updated\_at | TIMESTAMP | 是 | 更新時間 |

channel 允許值：

IN\_APP  
EMAIL  
PUSH

status 允許值：

PENDING  
SENT  
FAILED  
CLICKED

唯一限制：

unique(touch\_task\_id, member\_id, channel)

---

## **12.4 Table: member\_message**

用途：儲存站內信訊息。只有 IN\_APP channel 的 Touch Delivery 會建立 Member Message。

| 欄位 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| id | BIGSERIAL / BIGINT | 是 | Member Message ID，主鍵 |
| delivery\_id | BIGINT | 是 | 對應的 Touch Delivery ID |
| campaign\_id | BIGINT | 是 | 所屬 Campaign ID |
| member\_id | VARCHAR(100) | 是 | 會員 ID，來自外部會員系統 |
| title | VARCHAR(200) | 是 | 站內信標題 |
| content | TEXT | 是 | 站內信內容 |
| is\_read | BOOLEAN | 是 | 是否已讀 |
| clicked\_at | TIMESTAMP | 否 | 點擊時間 |
| created\_at | TIMESTAMP | 是 | 建立時間 |

唯一限制：

unique(delivery\_id)

---

## **12.5 Table: campaign\_event**

用途：記錄活動觸達過程中的事件。

| 欄位 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| id | BIGSERIAL / BIGINT | 是 | Campaign Event ID，主鍵 |
| campaign\_id | BIGINT | 是 | 所屬 Campaign ID |
| touch\_task\_id | BIGINT | 是 | 所屬 Touch Task ID |
| delivery\_id | BIGINT | 是 | 對應 Touch Delivery ID |
| member\_id | VARCHAR(100) | 是 | 會員 ID，來自外部會員系統 |
| event\_type | VARCHAR(20) | 是 | 事件類型 |
| channel | VARCHAR(20) | 是 | 觸達渠道 |
| occurred\_at | TIMESTAMP | 是 | 事件發生時間 |

event\_type 允許值：

SENT  
FAILED  
CLICK

唯一限制：

unique(delivery\_id, event\_type)

---

## **12.6 Relationship Summary**

campaign  
  └── touch\_task  
        └── touch\_delivery  
              ├── member\_message  
              └── campaign\_event

更完整關係：

campaign 1 \- N touch\_task  
campaign 1 \- N touch\_delivery  
campaign 1 \- N member\_message  
campaign 1 \- N campaign\_event

touch\_task 1 \- N touch\_delivery  
touch\_task 1 \- N campaign\_event

touch\_delivery 1 \- 0..1 member\_message  
touch\_delivery 1 \- N campaign\_event

重要說明：

`member_id` 只是外部會員系統的識別值。  
本系統不建立 member table，因此：

touch\_delivery.member\_id 不設外鍵到 member table  
member\_message.member\_id 不設外鍵到 member table  
campaign\_event.member\_id 不設外鍵到 member table

---

# **13\. API Contract**

## **13.1 General Rules**

Base URL：

http://localhost:8080

包含 Request Body 的 API 使用：

Content-Type: application/json

Admin API 需帶：

X-Admin-User: admin

Member API 需帶：

X-Member-Id: 1001

錯誤回應格式：

{  
  "code": "CAMPAIGN\_NOT\_FOUND",  
  "message": "Campaign not found."  
}

時間格式：

yyyy-MM-dd'T'HH:mm:ss

---

## **13.2 Error Code List**

| HTTP Status | Code | 說明 |
| ----- | ----- | ----- |
| 400 | VALIDATION\_ERROR | Request 欄位驗證失敗 |
| 400 | ADMIN\_USER\_REQUIRED | Admin API 缺少 X-Admin-User |
| 400 | MEMBER\_ID\_REQUIRED | Member API 缺少 X-Member-Id |
| 400 | INVALID\_CAMPAIGN\_STATUS | Campaign 狀態不允許該操作 |
| 400 | INVALID\_TOUCH\_TASK\_STATUS | Touch Task 狀態不允許該操作 |
| 400 | INVALID\_TOUCH\_DELIVERY\_STATUS | Touch Delivery 狀態不允許該操作 |
| 400 | INVALID\_AUDIENCE\_RULE | Audience Rule 格式錯誤 |
| 400 | UNSUPPORTED\_TOUCH\_CHANNEL | 不支援的 Touch Channel |
| 403 | MESSAGE\_NOT\_OWNED\_BY\_MEMBER | 會員操作非自己的站內信 |
| 404 | CAMPAIGN\_NOT\_FOUND | Campaign 不存在 |
| 404 | TOUCH\_TASK\_NOT\_FOUND | Touch Task 不存在 |
| 404 | MESSAGE\_NOT\_FOUND | Member Message 不存在 |
| 409 | DUPLICATE\_DELIVERY | 重複建立相同 delivery |
| 500 | INTERNAL\_ERROR | 系統未預期錯誤 |

---

# **14\. Admin API**

## **14.1 Create Campaign**

POST /api/admin/campaigns

Header：

X-Admin-User: admin  
Content-Type: application/json

Request：

{  
  "name": "618 3C 限時促銷",  
  "type": "PROMOTION",  
  "description": "618 期間針對 3C 商品的限時促銷活動。",  
  "landingPageUrl": "https://www.example.com/campaigns/618-3c",  
  "startTime": "2026-06-18T00:00:00",  
  "endTime": "2026-06-18T23:59:59"  
}

Validation：

| 欄位 | 型別 | 必填 | Validation |
| ----- | ----- | ----- | ----- |
| name | string | 是 | 1-100 字 |
| type | string | 是 | PROMOTION / NEW\_PRODUCT / MEMBER\_RECALL / CART\_REMINDER |
| description | string | 否 | 最多 2000 字 |
| landingPageUrl | string | 否 | 最多 500 字 |
| startTime | datetime | 是 | 不可為 null |
| endTime | datetime | 是 | 必須晚於 startTime |

Response 201：

{  
  "id": 1,  
  "name": "618 3C 限時促銷",  
  "type": "PROMOTION",  
  "description": "618 期間針對 3C 商品的限時促銷活動。",  
  "landingPageUrl": "https://www.example.com/campaigns/618-3c",  
  "startTime": "2026-06-18T00:00:00",  
  "endTime": "2026-06-18T23:59:59",  
  "status": "DRAFT",  
  "createdBy": "admin",  
  "createdAt": "2026-06-26T10:00:00",  
  "updatedAt": "2026-06-26T10:00:00"  
}

---

## **14.2 Get Campaign List**

GET /api/admin/campaigns

Header：

X-Admin-User: admin

Query Parameters：

| 參數 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| status | string | 否 | DRAFT / ACTIVE / PAUSED / ENDED |
| type | string | 否 | PROMOTION / NEW\_PRODUCT / MEMBER\_RECALL / CART\_REMINDER |

Response 200：

\[  
  {  
    "id": 1,  
    "name": "618 3C 限時促銷",  
    "type": "PROMOTION",  
    "startTime": "2026-06-18T00:00:00",  
    "endTime": "2026-06-18T23:59:59",  
    "status": "DRAFT",  
    "createdBy": "admin",  
    "createdAt": "2026-06-26T10:00:00",  
    "updatedAt": "2026-06-26T10:00:00"  
  }  
\]

---

## **14.3 Get Campaign Detail**

GET /api/admin/campaigns/{campaignId}

Response 200：

{  
  "id": 1,  
  "name": "618 3C 限時促銷",  
  "type": "PROMOTION",  
  "description": "618 期間針對 3C 商品的限時促銷活動。",  
  "landingPageUrl": "https://www.example.com/campaigns/618-3c",  
  "startTime": "2026-06-18T00:00:00",  
  "endTime": "2026-06-18T23:59:59",  
  "status": "DRAFT",  
  "createdBy": "admin",  
  "createdAt": "2026-06-26T10:00:00",  
  "updatedAt": "2026-06-26T10:00:00"  
}

---

## **14.4 Update Campaign**

PUT /api/admin/campaigns/{campaignId}

MVP 規則：

1. DRAFT 狀態可完整編輯。  
2. ACTIVE 狀態只允許編輯 description、landingPageUrl。  
3. PAUSED / ENDED 狀態不可編輯。

Request：

{  
  "name": "618 3C 限時促銷",  
  "type": "PROMOTION",  
  "description": "更新後的活動描述。",  
  "landingPageUrl": "https://www.example.com/campaigns/618-3c",  
  "startTime": "2026-06-18T00:00:00",  
  "endTime": "2026-06-18T23:59:59"  
}

---

## **14.5 Activate Campaign**

POST /api/admin/campaigns/{campaignId}/activate

允許狀態流轉：

DRAFT → ACTIVE  
PAUSED → ACTIVE

Response：

{  
  "id": 1,  
  "status": "ACTIVE",  
  "updatedAt": "2026-06-26T10:20:00"  
}

---

## **14.6 Pause Campaign**

POST /api/admin/campaigns/{campaignId}/pause

允許狀態流轉：

ACTIVE → PAUSED

---

## **14.7 End Campaign**

POST /api/admin/campaigns/{campaignId}/end

允許狀態流轉：

DRAFT → ENDED  
ACTIVE → ENDED  
PAUSED → ENDED

ENDED 後不可重新啟用。

---

## **14.8 Create Touch Task**

POST /api/admin/campaigns/{campaignId}/touch-tasks

MVP 規則：

1. DRAFT / ACTIVE Campaign 可以建立 Touch Task。  
2. PAUSED / ENDED Campaign 不可建立 Touch Task。  
3. Touch Task 建立後預設狀態為 PENDING。  
4. channels 至少需要一個值。  
5. audienceRule 使用固定欄位 JSON。  
6. MVP 不支援 scheduledTime。

Request：

{  
  "taskName": "618 3C 首波觸達",  
  "audienceRule": {  
    "memberLevels": \["VIP", "GOLD"\],  
    "lastLoginDaysLessThan": 30,  
    "favoriteCategories": \["3C"\],  
    "hasCartItems": true  
  },  
  "channels": \["IN\_APP", "PUSH"\],  
  "messageTitle": "618 3C 限時優惠",  
  "messageContent": "今晚 12 點前，指定 3C 商品限時優惠。"  
}

Validation：

| 欄位 | 型別 | 必填 | Validation |
| ----- | ----- | ----- | ----- |
| taskName | string | 是 | 1-100 字 |
| audienceRule | object | 是 | 只能包含 MVP 支援欄位 |
| channels | string array | 是 | 至少一個。允許 IN\_APP / EMAIL / PUSH |
| messageTitle | string | 是 | 1-200 字 |
| messageContent | string | 是 | 1-5000 字 |

Response 201：

{  
  "id": 10,  
  "campaignId": 1,  
  "taskName": "618 3C 首波觸達",  
  "audienceRule": {  
    "memberLevels": \["VIP", "GOLD"\],  
    "lastLoginDaysLessThan": 30,  
    "favoriteCategories": \["3C"\],  
    "hasCartItems": true  
  },  
  "channels": \["IN\_APP", "PUSH"\],  
  "messageTitle": "618 3C 限時優惠",  
  "messageContent": "今晚 12 點前，指定 3C 商品限時優惠。",  
  "status": "PENDING",  
  "executedAt": null,  
  "createdAt": "2026-06-26T11:00:00",  
  "updatedAt": "2026-06-26T11:00:00"  
}

---

## **14.9 Get Touch Tasks by Campaign**

GET /api/admin/campaigns/{campaignId}/touch-tasks

Response：

\[  
  {  
    "id": 10,  
    "campaignId": 1,  
    "taskName": "618 3C 首波觸達",  
    "channels": \["IN\_APP", "PUSH"\],  
    "status": "PENDING",  
    "executedAt": null,  
    "createdAt": "2026-06-26T11:00:00",  
    "updatedAt": "2026-06-26T11:00:00"  
  }  
\]

---

## **14.10 Get Touch Task Detail**

GET /api/admin/touch-tasks/{taskId}

---

## **14.11 Execute Touch Task**

POST /api/admin/touch-tasks/{taskId}/execute

執行流程：

1\. 檢查 Touch Task 是否存在。  
2\. 檢查 Touch Task 狀態是否為 PENDING。  
3\. 檢查所屬 Campaign 是否為 ACTIVE。  
4\. 將 Touch Task 狀態更新為 PROCESSING。  
5\. 呼叫 MockMemberProfileClient 查詢目標會員。  
6\. 根據 member \+ channel 建立 touch\_delivery。  
7\. 呼叫對應 Mock Touch Provider。  
8\. 更新 delivery 狀態為 SENT 或 FAILED。  
9\. IN\_APP channel 建立 member\_message。  
10\. 寫入 SENT / FAILED campaign\_event。  
11\. 將 Touch Task 狀態更新為 COMPLETED。

Response：

{  
  "touchTaskId": 10,  
  "campaignId": 1,  
  "status": "COMPLETED",  
  "targetMemberCount": 2,  
  "deliveryCount": 4,  
  "sentCount": 4,  
  "failedCount": 0,  
  "executedAt": "2026-06-26T11:10:00"  
}

---

## **14.12 Get Campaign Analytics**

GET /api/admin/campaigns/{campaignId}/analytics

Response：

{  
  "campaignId": 1,  
  "campaignName": "618 3C 限時促銷",  
  "targetMemberCount": 2,  
  "deliveryCount": 4,  
  "sentCount": 4,  
  "failedCount": 0,  
  "inAppSentCount": 2,  
  "clickCount": 1,  
  "clickThroughRate": 0.5  
}

---

# **15\. Member API**

## **15.1 Get Member Messages**

GET /api/member/messages

Header：

X-Member-Id: 1001

Query Parameters：

| 參數 | 型別 | 必填 | 說明 |
| ----- | ----- | ----- | ----- |
| campaignId | long | 否 | 依 Campaign 過濾 |
| unreadOnly | boolean | 否 | 是否只查未讀訊息 |

Response：

\[  
  {  
    "id": 100,  
    "deliveryId": 1000,  
    "campaignId": 1,  
    "title": "618 3C 限時優惠",  
    "content": "今晚 12 點前，指定 3C 商品限時優惠。",  
    "isRead": false,  
    "clickedAt": null,  
    "createdAt": "2026-06-26T11:10:00"  
  }  
\]

---

## **15.2 Click Member Message**

POST /api/member/messages/{messageId}/click

Header：

X-Member-Id: 1001

MVP 規則：

1. 只有站內信擁有者可以點擊該 message。  
2. 只有所屬 Campaign 為 ACTIVE 時，才記錄 CLICK。  
3. 第一次點擊時：  
   * member\_message.is\_read \= true  
   * member\_message.clicked\_at \= now  
   * touch\_delivery.status \= CLICKED  
   * touch\_delivery.clicked\_at \= now  
   * 新增 campaign\_event，eventType \= CLICK  
4. 重複點擊同一 message：  
   * 不重複新增 CLICK event  
   * 不重複計入 clickCount  
   * 可直接回傳目前 message 狀態

Response：

{  
  "messageId": 100,  
  "deliveryId": 1000,  
  "campaignId": 1,  
  "memberId": "1001",  
  "title": "618 3C 限時優惠",  
  "clickedAt": "2026-06-26T11:20:00",  
  "deliveryStatus": "CLICKED"  
}

---

# **16\. Service Flow**

## **16.1 Service Overview**

MVP 建議包含以下 Service：

CampaignService  
TouchTaskService  
TouchExecutionService  
MemberMessageService  
CampaignAnalyticsService  
MemberProfileClient  
TouchProvider

---

## **16.2 CampaignService**

負責 Campaign 的建立、查詢、更新與狀態流轉。

主要責任：

1\. 驗證 Campaign 是否存在。  
2\. 驗證 Campaign 時間是否合法。  
3\. 控制 Campaign 狀態流轉。  
4\. 控制不同狀態下可修改的欄位。  
5\. 不處理觸達邏輯。  
6\. 不處理會員、商品、訂單、優惠券邏輯。

---

## **16.3 TouchTaskService**

負責 Touch Task 的建立與查詢。

主要責任：

1\. 驗證 Campaign 是否存在。  
2\. 驗證 Campaign 是否允許建立 Touch Task。  
3\. 驗證 Audience Rule 格式。  
4\. 驗證 Touch Channel 是否支援。  
5\. 建立 PENDING 狀態的 Touch Task。  
6\. 不執行觸達派送。

---

## **16.4 TouchExecutionService**

負責手動執行 Touch Task。

主要責任：

1\. 驗證 Touch Task 是否存在。  
2\. 驗證 Touch Task 狀態是否為 PENDING。  
3\. 驗證 Campaign 狀態是否為 ACTIVE。  
4\. 將 Touch Task 狀態更新為 PROCESSING。  
5\. 呼叫 MemberProfileClient 查詢目標會員。  
6\. 根據 member \+ channel 建立 Touch Delivery。  
7\. 呼叫 TouchProvider 模擬派送。  
8\. 更新 Touch Delivery 狀態。  
9\. IN\_APP channel 建立 Member Message。  
10\. 寫入 Campaign Event。  
11\. 將 Touch Task 狀態更新為 COMPLETED 或 FAILED。

交易邊界：

executeTouchTask 使用單一 transaction。

MVP 不引入 RabbitMQ、不做分散式派送，因此使用單一 transaction 簡化一致性處理。

若部分 Provider 回傳 FAILED，Touch Task 仍為 COMPLETED。  
Touch Task 的 COMPLETED 代表任務流程完成，不代表每筆 delivery 都成功。

---

## **16.5 MemberMessageService**

負責會員查詢站內信與點擊站內信。

主要責任：

1\. 根據 X-Member-Id 查詢會員自己的站內信。  
2\. 驗證會員不可操作他人的站內信。  
3\. 驗證 Campaign 是否為 ACTIVE。  
4\. 第一次點擊時，更新 Member Message 與 Touch Delivery。  
5\. 第一次點擊時，新增 CLICK Campaign Event。  
6\. 重複點擊時，不重複新增 CLICK Event。

---

## **16.6 CampaignAnalyticsService**

負責查詢 Campaign 觸達成效。

主要責任：

1\. 驗證 Campaign 是否存在。  
2\. 從 Touch Delivery 統計活動成效。  
3\. 計算 targetMemberCount。  
4\. 計算 deliveryCount。  
5\. 計算 sentCount。  
6\. 計算 failedCount。  
7\. 計算 inAppSentCount。  
8\. 計算 clickCount。  
9\. 計算 clickThroughRate。

Analytics 以 touch\_delivery 為主要統計來源。  
Campaign Event 可作為事件紀錄與輔助查詢，但 MVP 成效數字以 touch\_delivery 為準。

---

# **17\. Mock External Client and Provider**

## **17.1 MemberProfileClient**

概念設計：

public interface MemberProfileClient {  
    List\<TouchTargetMember\> queryTargetMembers(AudienceRuleCriteria criteria);  
}

MVP 實作：

MockMemberProfileClient

MockMemberProfileClient 需根據 Audience Rule 過濾 mock members。

Filtering Rules：

1\. memberLevels 為 null 或空陣列：不限會員等級。  
2\. memberLevels 有值：member.memberLevel 必須在清單內。  
3\. lastLoginDaysLessThan 為 null：不限登入天數。  
4\. lastLoginDaysLessThan 有值：member.lastLoginAt 必須在最近 N 天內。  
5\. favoriteCategories 為 null 或空陣列：不限分類。  
6\. favoriteCategories 有值：member.favoriteCategories 至少命中一個分類。  
7\. hasCartItems 為 null：不限購物車狀態。  
8\. hasCartItems \= true：member.hasCartItems 必須為 true。  
9\. hasCartItems \= false：member.hasCartItems 必須為 false。

---

## **17.2 TouchProvider**

概念設計：

public interface TouchProvider {  
    TouchChannel supportChannel();  
    DeliveryResult send(TouchMessageCommand command);  
}

MVP 實作：

InAppTouchProvider  
MockEmailTouchProvider  
MockPushTouchProvider

Provider 責任邊界：

Provider 只回傳 DeliveryResult。  
Provider 不直接建立或更新 touch\_delivery。  
Provider 不直接建立 campaign\_event。  
TouchExecutionService 負責建立 member\_message。

---

# **18\. Technical Stack**

MVP 使用：

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

MVP 不使用：

Redis  
RabbitMQ  
Kafka  
Vue  
React  
Spring Security  
JWT  
OAuth2  
Elasticsearch

Maven Dependencies：

spring-boot-starter-web  
spring-boot-starter-data-jpa  
spring-boot-starter-validation  
springdoc-openapi-starter-webmvc-ui  
postgresql  
flyway-core  
jackson-databind  
spring-boot-starter-test

可選：

lombok

---

# **19\. Package Structure**

Root package：

com.example.campaigntouch

建議目錄：

backend/  
 ├── pom.xml  
 ├── src/  
 │   ├── main/  
 │   │   ├── java/  
 │   │   │   └── com/example/campaigntouch/  
 │   │   │       ├── CampaignTouchApplication.java  
 │   │   │       ├── common/  
 │   │   │       ├── campaign/  
 │   │   │       ├── touch/  
 │   │   │       ├── message/  
 │   │   │       ├── analytics/  
 │   │   │       ├── external/  
 │   │   │       └── config/  
 │   │   └── resources/  
 │   │       ├── application.yml  
 │   │       ├── db/migration/V1\_\_init\_schema.sql  
 │   │       └── mock/members.json  
 │   └── test/

---

## **19.1 common package**

common/  
 ├── error/  
 │   ├── ErrorCode.java  
 │   ├── ErrorResponse.java  
 │   ├── BusinessException.java  
 │   └── GlobalExceptionHandler.java  
 ├── util/  
 │   └── DateTimeProvider.java  
 └── validation/

---

## **19.2 campaign package**

campaign/  
 ├── controller/  
 │   └── CampaignAdminController.java  
 ├── service/  
 │   └── CampaignService.java  
 ├── repository/  
 │   └── CampaignRepository.java  
 ├── entity/  
 │   └── Campaign.java  
 ├── dto/  
 │   ├── CreateCampaignRequest.java  
 │   ├── UpdateCampaignRequest.java  
 │   ├── CampaignResponse.java  
 │   ├── CampaignSummaryResponse.java  
 │   └── CampaignStatusResponse.java  
 └── enums/  
     ├── CampaignStatus.java  
     └── CampaignType.java

---

## **19.3 touch package**

touch/  
 ├── controller/  
 │   └── TouchTaskAdminController.java  
 ├── service/  
 │   ├── TouchTaskService.java  
 │   └── TouchExecutionService.java  
 ├── repository/  
 │   ├── TouchTaskRepository.java  
 │   └── TouchDeliveryRepository.java  
 ├── entity/  
 │   ├── TouchTask.java  
 │   └── TouchDelivery.java  
 ├── dto/  
 │   ├── CreateTouchTaskRequest.java  
 │   ├── TouchTaskResponse.java  
 │   ├── TouchTaskSummaryResponse.java  
 │   ├── TouchExecutionResponse.java  
 │   ├── AudienceRuleCriteria.java  
 │   └── TouchMessageCommand.java  
 ├── enums/  
 │   ├── TouchTaskStatus.java  
 │   ├── TouchChannel.java  
 │   └── TouchDeliveryStatus.java  
 └── provider/  
     ├── TouchProvider.java  
     ├── DeliveryResult.java  
     ├── InAppTouchProvider.java  
     ├── MockEmailTouchProvider.java  
     └── MockPushTouchProvider.java

---

## **19.4 message package**

message/  
 ├── controller/  
 │   └── MemberMessageController.java  
 ├── service/  
 │   └── MemberMessageService.java  
 ├── repository/  
 │   └── MemberMessageRepository.java  
 ├── entity/  
 │   └── MemberMessage.java  
 └── dto/  
     ├── MemberMessageResponse.java  
     └── MemberMessageClickResponse.java

---

## **19.5 analytics package**

analytics/  
 ├── controller/  
 │   └── CampaignAnalyticsAdminController.java  
 ├── service/  
 │   └── CampaignAnalyticsService.java  
 └── dto/  
     └── CampaignAnalyticsResponse.java

---

## **19.6 external package**

external/  
 └── member/  
     ├── MemberProfileClient.java  
     ├── MockMemberProfileClient.java  
     └── TouchTargetMember.java

---

# **20\. Flyway Migration SQL v1**

檔案位置：

backend/src/main/resources/db/migration/V1\_\_init\_schema.sql

SQL：

CREATE TABLE campaign (  
    id BIGSERIAL PRIMARY KEY,  
    name VARCHAR(100) NOT NULL,  
    type VARCHAR(50) NOT NULL,  
    description TEXT,  
    landing\_page\_url VARCHAR(500),  
    start\_time TIMESTAMP NOT NULL,  
    end\_time TIMESTAMP NOT NULL,  
    status VARCHAR(20) NOT NULL,  
    created\_by VARCHAR(100) NOT NULL,  
    created\_at TIMESTAMP NOT NULL,  
    updated\_at TIMESTAMP NOT NULL,

    CONSTRAINT chk\_campaign\_time CHECK (end\_time \> start\_time),  
    CONSTRAINT chk\_campaign\_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED')),  
    CONSTRAINT chk\_campaign\_type CHECK (type IN ('PROMOTION', 'NEW\_PRODUCT', 'MEMBER\_RECALL', 'CART\_REMINDER'))  
);

CREATE INDEX idx\_campaign\_status ON campaign(status);  
CREATE INDEX idx\_campaign\_start\_time ON campaign(start\_time);  
CREATE INDEX idx\_campaign\_end\_time ON campaign(end\_time);

CREATE TABLE touch\_task (  
    id BIGSERIAL PRIMARY KEY,  
    campaign\_id BIGINT NOT NULL,  
    task\_name VARCHAR(100) NOT NULL,  
    audience\_rule\_json JSONB NOT NULL,  
    channels\_json JSONB NOT NULL,  
    message\_title VARCHAR(200) NOT NULL,  
    message\_content TEXT NOT NULL,  
    status VARCHAR(20) NOT NULL,  
    executed\_at TIMESTAMP,  
    created\_at TIMESTAMP NOT NULL,  
    updated\_at TIMESTAMP NOT NULL,

    CONSTRAINT fk\_touch\_task\_campaign  
        FOREIGN KEY (campaign\_id)  
        REFERENCES campaign(id),

    CONSTRAINT chk\_touch\_task\_status  
        CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED')),

    CONSTRAINT chk\_touch\_task\_channels\_json\_array  
        CHECK (jsonb\_typeof(channels\_json) \= 'array'),

    CONSTRAINT chk\_touch\_task\_audience\_rule\_json\_object  
        CHECK (jsonb\_typeof(audience\_rule\_json) \= 'object')  
);

CREATE INDEX idx\_touch\_task\_campaign\_id ON touch\_task(campaign\_id);  
CREATE INDEX idx\_touch\_task\_status ON touch\_task(status);

CREATE TABLE touch\_delivery (  
    id BIGSERIAL PRIMARY KEY,  
    touch\_task\_id BIGINT NOT NULL,  
    campaign\_id BIGINT NOT NULL,  
    member\_id VARCHAR(100) NOT NULL,  
    channel VARCHAR(20) NOT NULL,  
    title VARCHAR(200) NOT NULL,  
    content TEXT NOT NULL,  
    status VARCHAR(20) NOT NULL,  
    sent\_at TIMESTAMP,  
    clicked\_at TIMESTAMP,  
    failed\_reason VARCHAR(500),  
    created\_at TIMESTAMP NOT NULL,  
    updated\_at TIMESTAMP NOT NULL,

    CONSTRAINT fk\_touch\_delivery\_touch\_task  
        FOREIGN KEY (touch\_task\_id)  
        REFERENCES touch\_task(id),

    CONSTRAINT fk\_touch\_delivery\_campaign  
        FOREIGN KEY (campaign\_id)  
        REFERENCES campaign(id),

    CONSTRAINT chk\_touch\_delivery\_channel  
        CHECK (channel IN ('IN\_APP', 'EMAIL', 'PUSH')),

    CONSTRAINT chk\_touch\_delivery\_status  
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CLICKED')),

    CONSTRAINT uk\_touch\_delivery\_task\_member\_channel  
        UNIQUE (touch\_task\_id, member\_id, channel)  
);

CREATE INDEX idx\_touch\_delivery\_campaign\_id ON touch\_delivery(campaign\_id);  
CREATE INDEX idx\_touch\_delivery\_touch\_task\_id ON touch\_delivery(touch\_task\_id);  
CREATE INDEX idx\_touch\_delivery\_member\_id ON touch\_delivery(member\_id);  
CREATE INDEX idx\_touch\_delivery\_channel ON touch\_delivery(channel);  
CREATE INDEX idx\_touch\_delivery\_status ON touch\_delivery(status);  
CREATE INDEX idx\_touch\_delivery\_campaign\_channel\_status  
    ON touch\_delivery(campaign\_id, channel, status);

CREATE TABLE member\_message (  
    id BIGSERIAL PRIMARY KEY,  
    delivery\_id BIGINT NOT NULL,  
    campaign\_id BIGINT NOT NULL,  
    member\_id VARCHAR(100) NOT NULL,  
    title VARCHAR(200) NOT NULL,  
    content TEXT NOT NULL,  
    is\_read BOOLEAN NOT NULL DEFAULT FALSE,  
    clicked\_at TIMESTAMP,  
    created\_at TIMESTAMP NOT NULL,

    CONSTRAINT fk\_member\_message\_delivery  
        FOREIGN KEY (delivery\_id)  
        REFERENCES touch\_delivery(id),

    CONSTRAINT fk\_member\_message\_campaign  
        FOREIGN KEY (campaign\_id)  
        REFERENCES campaign(id),

    CONSTRAINT uk\_member\_message\_delivery  
        UNIQUE (delivery\_id)  
);

CREATE INDEX idx\_member\_message\_member\_id ON member\_message(member\_id);  
CREATE INDEX idx\_member\_message\_campaign\_id ON member\_message(campaign\_id);  
CREATE INDEX idx\_member\_message\_delivery\_id ON member\_message(delivery\_id);  
CREATE INDEX idx\_member\_message\_member\_created\_at  
    ON member\_message(member\_id, created\_at DESC);

CREATE TABLE campaign\_event (  
    id BIGSERIAL PRIMARY KEY,  
    campaign\_id BIGINT NOT NULL,  
    touch\_task\_id BIGINT NOT NULL,  
    delivery\_id BIGINT NOT NULL,  
    member\_id VARCHAR(100) NOT NULL,  
    event\_type VARCHAR(20) NOT NULL,  
    channel VARCHAR(20) NOT NULL,  
    occurred\_at TIMESTAMP NOT NULL,

    CONSTRAINT fk\_campaign\_event\_campaign  
        FOREIGN KEY (campaign\_id)  
        REFERENCES campaign(id),

    CONSTRAINT fk\_campaign\_event\_touch\_task  
        FOREIGN KEY (touch\_task\_id)  
        REFERENCES touch\_task(id),

    CONSTRAINT fk\_campaign\_event\_delivery  
        FOREIGN KEY (delivery\_id)  
        REFERENCES touch\_delivery(id),

    CONSTRAINT chk\_campaign\_event\_type  
        CHECK (event\_type IN ('SENT', 'FAILED', 'CLICK')),

    CONSTRAINT chk\_campaign\_event\_channel  
        CHECK (channel IN ('IN\_APP', 'EMAIL', 'PUSH')),

    CONSTRAINT uk\_campaign\_event\_delivery\_event\_type  
        UNIQUE (delivery\_id, event\_type)  
);

CREATE INDEX idx\_campaign\_event\_campaign\_id ON campaign\_event(campaign\_id);  
CREATE INDEX idx\_campaign\_event\_touch\_task\_id ON campaign\_event(touch\_task\_id);  
CREATE INDEX idx\_campaign\_event\_delivery\_id ON campaign\_event(delivery\_id);  
CREATE INDEX idx\_campaign\_event\_event\_type ON campaign\_event(event\_type);  
CREATE INDEX idx\_campaign\_event\_occurred\_at ON campaign\_event(occurred\_at);

---

# **21\. application.yml Spec**

檔案位置：

backend/src/main/resources/application.yml

建議內容：

server:  
  port: 8080

spring:  
  application:  
    name: campaign-touch-system

  datasource:  
    url: jdbc:postgresql://localhost:5432/campaign\_touch  
    username: campaign\_user  
    password: campaign\_pass  
    driver-class-name: org.postgresql.Driver

  jpa:  
    hibernate:  
      ddl-auto: validate  
    open-in-view: false  
    properties:  
      hibernate:  
        format\_sql: true  
        jdbc:  
          time\_zone: Asia/Taipei

  flyway:  
    enabled: true  
    locations: classpath:db/migration  
    baseline-on-migrate: true

springdoc:  
  swagger-ui:  
    path: /swagger-ui.html  
  api-docs:  
    path: /v3/api-docs

logging:  
  level:  
    org.hibernate.SQL: debug  
    org.hibernate.orm.jdbc.bind: trace  
    com.example.campaigntouch: debug

app:  
  mock:  
    member-profile:  
      resource-path: classpath:mock/members.json

規則：

Hibernate ddl-auto 必須為 validate。  
不得使用 create 或 update。  
資料表需由 Flyway 管理。

---

# **22\. docker-compose.yml Spec**

檔案位置：

docker-compose.yml

MVP 至少需要 PostgreSQL：

services:  
  postgres:  
    image: postgres:16  
    container\_name: campaign-touch-postgres  
    environment:  
      POSTGRES\_DB: campaign\_touch  
      POSTGRES\_USER: campaign\_user  
      POSTGRES\_PASSWORD: campaign\_pass  
    ports:  
      \- "5432:5432"  
    volumes:  
      \- campaign\_touch\_pgdata:/var/lib/postgresql/data  
    healthcheck:  
      test: \["CMD-SHELL", "pg\_isready \-U campaign\_user \-d campaign\_touch"\]  
      interval: 10s  
      timeout: 5s  
      retries: 5

volumes:  
  campaign\_touch\_pgdata:

MVP 不得啟動 Redis、RabbitMQ、Kafka。

---

# **23\. Mock Member Data**

檔案位置：

backend/src/main/resources/mock/members.json

範例：

\[  
  {  
    "memberId": "1001",  
    "email": "user1001@example.com",  
    "pushToken": "push-token-1001",  
    "memberLevel": "VIP",  
    "favoriteCategories": \["3C", "Mobile"\],  
    "lastLoginAt": "2026-06-20T10:00:00",  
    "hasCartItems": true  
  },  
  {  
    "memberId": "1002",  
    "email": "user1002@example.com",  
    "pushToken": "push-token-1002",  
    "memberLevel": "GOLD",  
    "favoriteCategories": \["3C"\],  
    "lastLoginAt": "2026-06-22T10:00:00",  
    "hasCartItems": true  
  },  
  {  
    "memberId": "1003",  
    "email": "user1003@example.com",  
    "pushToken": "push-token-1003",  
    "memberLevel": "NORMAL",  
    "favoriteCategories": \["Fashion"\],  
    "lastLoginAt": "2026-05-01T10:00:00",  
    "hasCartItems": false  
  }  
\]

注意：

members.json 只是模擬既有會員系統的回傳資料。  
不得因此建立 member table。  
不得提供會員 CRUD API。

---

# **24\. AI Implementation Guide**

AI 實作時應依照以下順序：

1. 建立 Spring Boot 專案骨架。  
2. 建立 Common Error Handling。  
3. 建立 Enum。  
4. 建立 Entity 與 Flyway Migration。  
5. 建立 Repository。  
6. 建立 DTO。  
7. 建立 Mock External Client。  
8. 建立 Touch Provider。  
9. 建立 CampaignService \+ CampaignAdminController。  
10. 建立 TouchTaskService \+ TouchTaskAdminController。  
11. 建立 TouchExecutionService。  
12. 建立 MemberMessageService \+ MemberMessageController。  
13. 建立 CampaignAnalyticsService \+ Controller。  
14. 建立 Swagger / OpenAPI。  
15. 建立 Docker Compose。  
16. 建立 README。  
17. 建立 JUnit 測試。

---

## **24.1 AI Guardrails**

AI 實作時必須遵守：

1\. 不得實作會員管理。  
2\. 不得建立 member table。  
3\. 不得建立商品、訂單、優惠券、付款、購物車、庫存相關資料表。  
4\. 不得使用 Redis / RabbitMQ / Kafka。  
5\. 不得串接真實第三方服務。  
6\. 不得新增前端。  
7\. 不得新增登入系統。  
8\. 不得把 Phase 2 功能放入 MVP。  
9\. 不得把 mock member data 視為本系統正式資料。  
10\. 不得讓 EMAIL / PUSH 產生 Member Message。  
11\. 不得讓 EMAIL / PUSH 支援 click tracking。  
12\. 不得用 campaign\_event 作為 Analytics 主要統計來源。  
13\. 不得讓 Touch Task 重複 execute。  
14\. 不得讓 ENDED Campaign 重新啟用。

---

# **25\. Testing Guide**

MVP 至少需提供以下測試。

## **25.1 CampaignServiceTest**

createCampaign\_shouldCreateDraftCampaign  
createCampaign\_shouldRejectInvalidTimeRange  
activateCampaign\_shouldActivateDraftCampaign  
activateCampaign\_shouldRejectEndedCampaign  
updateCampaign\_shouldRejectPausedCampaign

---

## **25.2 TouchTaskServiceTest**

createTouchTask\_shouldCreatePendingTask  
createTouchTask\_shouldRejectPausedCampaign  
createTouchTask\_shouldRejectUnsupportedChannel  
createTouchTask\_shouldRejectDuplicatedChannels  
createTouchTask\_shouldRejectInvalidAudienceRule

---

## **25.3 TouchExecutionServiceTest**

executeTouchTask\_shouldCreateDeliveries  
executeTouchTask\_shouldCreateMemberMessagesForInAppOnly  
executeTouchTask\_shouldCreateSentEvents  
executeTouchTask\_shouldRejectNonPendingTask  
executeTouchTask\_shouldRejectInactiveCampaign  
executeTouchTask\_shouldMarkTaskCompletedWhenPartialDeliveryFailed

---

## **25.4 MemberMessageServiceTest**

getMemberMessages\_shouldReturnOnlyCurrentMemberMessages  
clickMessage\_shouldUpdateDeliveryToClicked  
clickMessage\_shouldCreateClickEvent  
clickMessage\_shouldRejectOtherMemberMessage  
clickMessage\_shouldNotDuplicateClickEventWhenRepeated  
clickMessage\_shouldRejectInactiveCampaign

---

## **25.5 CampaignAnalyticsServiceTest**

getCampaignAnalytics\_shouldCalculateTargetMemberCount  
getCampaignAnalytics\_shouldCalculateDeliveryCount  
getCampaignAnalytics\_shouldIncludeClickedInSentCount  
getCampaignAnalytics\_shouldCalculateClickThroughRateByInAppSentCount  
getCampaignAnalytics\_shouldReturnZeroClickThroughRateWhenNoInAppSent

---

# **26\. Local Run Guide**

## **26.1 Start PostgreSQL**

docker compose up \-d postgres

---

## **26.2 Run Backend**

cd backend  
./mvnw spring-boot:run

或：

mvn spring-boot:run

---

## **26.3 Swagger UI**

啟動後可開啟：

http://localhost:8080/swagger-ui.html

或：

http://localhost:8080/swagger-ui/index.html

---

## **26.4 Verify Database**

資料庫中應存在：

campaign  
touch\_task  
touch\_delivery  
member\_message  
campaign\_event  
flyway\_schema\_history

不應存在：

member  
product  
order  
coupon  
payment  
cart  
inventory

---

# **27\. Demo Script**

Demo 情境：

活動名稱：618 3C 限時促銷  
活動類型：PROMOTION  
觸達客群：  
  \- 會員等級：VIP / GOLD  
  \- 最近 30 天內登入  
  \- 偏好分類包含 3C  
  \- 有購物車商品  
觸達渠道：  
  \- IN\_APP  
  \- PUSH  
訊息標題：  
  618 3C 限時優惠  
訊息內容：  
  今晚 12 點前，指定 3C 商品限時優惠。

---

## **Step 1：Create Campaign**

curl \-X POST http://localhost:8080/api/admin/campaigns \\  
  \-H "Content-Type: application/json" \\  
  \-H "X-Admin-User: admin" \\  
  \-d '{  
    "name": "618 3C 限時促銷",  
    "type": "PROMOTION",  
    "description": "618 期間針對 3C 商品的限時促銷活動。",  
    "landingPageUrl": "https://www.example.com/campaigns/618-3c",  
    "startTime": "2026-06-18T00:00:00",  
    "endTime": "2026-06-18T23:59:59"  
  }'

---

## **Step 2：Activate Campaign**

curl \-X POST http://localhost:8080/api/admin/campaigns/1/activate \\  
  \-H "X-Admin-User: admin"

---

## **Step 3：Create Touch Task**

curl \-X POST http://localhost:8080/api/admin/campaigns/1/touch-tasks \\  
  \-H "Content-Type: application/json" \\  
  \-H "X-Admin-User: admin" \\  
  \-d '{  
    "taskName": "618 3C 首波觸達",  
    "audienceRule": {  
      "memberLevels": \["VIP", "GOLD"\],  
      "lastLoginDaysLessThan": 30,  
      "favoriteCategories": \["3C"\],  
      "hasCartItems": true  
    },  
    "channels": \["IN\_APP", "PUSH"\],  
    "messageTitle": "618 3C 限時優惠",  
    "messageContent": "今晚 12 點前，指定 3C 商品限時優惠。"  
  }'

---

## **Step 4：Execute Touch Task**

curl \-X POST http://localhost:8080/api/admin/touch-tasks/10/execute \\  
  \-H "X-Admin-User: admin"

預期：

{  
  "touchTaskId": 10,  
  "campaignId": 1,  
  "status": "COMPLETED",  
  "targetMemberCount": 2,  
  "deliveryCount": 4,  
  "sentCount": 4,  
  "failedCount": 0  
}

---

## **Step 5：Member Query Messages**

curl \-X GET http://localhost:8080/api/member/messages \\  
  \-H "X-Member-Id: 1001"

---

## **Step 6：Member Click Message**

curl \-X POST http://localhost:8080/api/member/messages/100/click \\  
  \-H "X-Member-Id: 1001"

---

## **Step 7：Query Campaign Analytics**

curl \-X GET http://localhost:8080/api/admin/campaigns/1/analytics \\  
  \-H "X-Admin-User: admin"

預期：

{  
  "campaignId": 1,  
  "campaignName": "618 3C 限時促銷",  
  "targetMemberCount": 2,  
  "deliveryCount": 4,  
  "sentCount": 4,  
  "failedCount": 0,  
  "inAppSentCount": 2,  
  "clickCount": 1,  
  "clickThroughRate": 0.5  
}

---

# **28\. Negative Test Scenarios**

## **28.1 Missing Admin Header**

預期：

{  
  "code": "ADMIN\_USER\_REQUIRED",  
  "message": "X-Admin-User header is required."  
}

---

## **28.2 Invalid Campaign Time**

預期：

{  
  "code": "VALIDATION\_ERROR",  
  "message": "endTime must be after startTime."  
}

---

## **28.3 Execute Touch Task Before Campaign Active**

預期：

{  
  "code": "INVALID\_CAMPAIGN\_STATUS",  
  "message": "Only ACTIVE campaign can execute touch task."  
}

---

## **28.4 Execute Completed Touch Task Again**

預期：

{  
  "code": "INVALID\_TOUCH\_TASK\_STATUS",  
  "message": "Only PENDING touch task can be executed."  
}

---

## **28.5 Member Click Other Member's Message**

預期：

{  
  "code": "MESSAGE\_NOT\_OWNED\_BY\_MEMBER",  
  "message": "This message does not belong to current member."  
}

---

## **28.6 Missing Member Header**

預期：

{  
  "code": "MEMBER\_ID\_REQUIRED",  
  "message": "X-Member-Id header is required."  
}

---

# **29\. MVP Acceptance Criteria**

## **29.1 Scope**

\[ \] 沒有 member table  
\[ \] 沒有 product table  
\[ \] 沒有 order table  
\[ \] 沒有 coupon table  
\[ \] 沒有 payment table  
\[ \] 沒有 cart table  
\[ \] 沒有 inventory table  
\[ \] 沒有 Redis  
\[ \] 沒有 RabbitMQ  
\[ \] 沒有 Kafka  
\[ \] 沒有真實 Email / Push 串接  
\[ \] 沒有 Vue / React 前端

---

## **29.2 Core Flow**

\[ \] 可以建立 Campaign  
\[ \] 可以 activate Campaign  
\[ \] 可以建立 Touch Task  
\[ \] 可以 execute Touch Task  
\[ \] execute 後產生 touch\_delivery  
\[ \] IN\_APP 產生 member\_message  
\[ \] PUSH / EMAIL 不產生 member\_message  
\[ \] 會員可以查詢自己的站內信  
\[ \] 會員可以點擊自己的站內信  
\[ \] 重複點擊不重複計算 click  
\[ \] 可以查詢 Campaign Analytics

---

## **29.3 Technical**

\[ \] 使用 Java 17  
\[ \] 使用 Spring Boot 3.x  
\[ \] 使用 PostgreSQL  
\[ \] 使用 Flyway  
\[ \] 使用 Swagger UI  
\[ \] 使用 Docker Compose 啟動 PostgreSQL  
\[ \] Hibernate ddl-auto \= validate  
\[ \] 有 JUnit 測試  
\[ \] README 有完整 Demo Flow

---

# **30\. Known Limitations**

MVP 已知限制：

1\. 不支援真實 Email / Push 派送。  
2\. 不支援排程自動派送。  
3\. 不支援 RabbitMQ 非同步削峰。  
4\. 不支援 Redis 快取。  
5\. 不支援優惠券發放。  
6\. 不支援訂單轉換追蹤。  
7\. 不支援 GMV 統計。  
8\. 不支援完整會員登入。  
9\. 不支援前端後台。  
10\. Mock member data 僅作為外部會員系統回傳資料的模擬。

---

# **31\. Final Notes**

本專案重點不是重做完整電商平台，而是聚焦在「活動觸達任務」這個新增服務。

設計重點：

Campaign 定義活動  
Touch Task 定義觸達任務  
Audience Rule 定義觸達條件  
Mock Member Profile Client 模擬既有會員系統  
Touch Delivery 記錄每一筆觸達  
Member Message 支援站內信查詢與點擊  
Campaign Analytics 統計觸達成效

所有 MVP 以外功能都應保留在 Future Scope，不應在本版本中實作。


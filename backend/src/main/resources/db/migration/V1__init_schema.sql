CREATE TABLE campaign (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    description TEXT,
    landing_page_url VARCHAR(500),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_by VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_campaign_type CHECK (type IN ('PROMOTION', 'NEW_PRODUCT', 'MEMBER_RECALL', 'CART_REMINDER')),
    CONSTRAINT ck_campaign_status CHECK (status IN ('DRAFT', 'ACTIVE', 'PAUSED', 'ENDED')),
    CONSTRAINT ck_campaign_time_range CHECK (end_time > start_time)
);

CREATE TABLE touch_task (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    task_name VARCHAR(100) NOT NULL,
    audience_rule_json JSONB NOT NULL,
    channels_json JSONB NOT NULL,
    message_title VARCHAR(200) NOT NULL,
    message_content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    executed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_touch_task_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id),
    CONSTRAINT ck_touch_task_status CHECK (status IN ('PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE touch_delivery (
    id BIGSERIAL PRIMARY KEY,
    touch_task_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    clicked_at TIMESTAMP,
    failed_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_touch_delivery_task FOREIGN KEY (touch_task_id) REFERENCES touch_task (id),
    CONSTRAINT fk_touch_delivery_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id),
    CONSTRAINT uq_touch_delivery_task_member_channel UNIQUE (touch_task_id, member_id, channel),
    CONSTRAINT ck_touch_delivery_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    CONSTRAINT ck_touch_delivery_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CLICKED'))
);

CREATE TABLE member_message (
    id BIGSERIAL PRIMARY KEY,
    delivery_id BIGINT NOT NULL,
    campaign_id BIGINT NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    clicked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_message_delivery FOREIGN KEY (delivery_id) REFERENCES touch_delivery (id),
    CONSTRAINT fk_member_message_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id),
    CONSTRAINT uq_member_message_delivery UNIQUE (delivery_id)
);

CREATE TABLE campaign_event (
    id BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT NOT NULL,
    touch_task_id BIGINT NOT NULL,
    delivery_id BIGINT NOT NULL,
    member_id VARCHAR(100) NOT NULL,
    event_type VARCHAR(20) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_campaign_event_campaign FOREIGN KEY (campaign_id) REFERENCES campaign (id),
    CONSTRAINT fk_campaign_event_task FOREIGN KEY (touch_task_id) REFERENCES touch_task (id),
    CONSTRAINT fk_campaign_event_delivery FOREIGN KEY (delivery_id) REFERENCES touch_delivery (id),
    CONSTRAINT uq_campaign_event_delivery_type UNIQUE (delivery_id, event_type),
    CONSTRAINT ck_campaign_event_type CHECK (event_type IN ('SENT', 'FAILED', 'CLICK')),
    CONSTRAINT ck_campaign_event_channel CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH'))
);

CREATE INDEX idx_touch_task_campaign_id ON touch_task (campaign_id);
CREATE INDEX idx_touch_delivery_campaign_id ON touch_delivery (campaign_id);
CREATE INDEX idx_touch_delivery_task_id ON touch_delivery (touch_task_id);
CREATE INDEX idx_member_message_member_id ON member_message (member_id);
CREATE INDEX idx_campaign_event_campaign_id ON campaign_event (campaign_id);

package com.example.campaigntouch.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
    ADMIN_USER_REQUIRED(HttpStatus.BAD_REQUEST, "Admin user is required."),
    MEMBER_ID_REQUIRED(HttpStatus.BAD_REQUEST, "Member id is required."),
    CAMPAIGN_NOT_FOUND(HttpStatus.NOT_FOUND, "Campaign not found."),
    TOUCH_TASK_NOT_FOUND(HttpStatus.NOT_FOUND, "Touch task not found."),
    MESSAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "Member message not found."),
    INVALID_CAMPAIGN_STATUS(HttpStatus.BAD_REQUEST, "Campaign status does not allow this operation."),
    INVALID_TOUCH_TASK_STATUS(HttpStatus.BAD_REQUEST, "Touch task status does not allow this operation."),
    INVALID_TOUCH_DELIVERY_STATUS(HttpStatus.BAD_REQUEST, "Touch delivery status does not allow this operation."),
    INVALID_AUDIENCE_RULE(HttpStatus.BAD_REQUEST, "Audience rule is invalid."),
    UNSUPPORTED_TOUCH_CHANNEL(HttpStatus.BAD_REQUEST, "Touch channel is not supported."),
    MESSAGE_NOT_OWNED_BY_MEMBER(HttpStatus.FORBIDDEN, "Message is not owned by member."),
    DUPLICATE_DELIVERY(HttpStatus.CONFLICT, "Duplicate delivery."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

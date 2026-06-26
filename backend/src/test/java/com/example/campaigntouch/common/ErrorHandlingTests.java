package com.example.campaigntouch.common;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class ErrorHandlingTests {

    @Test
    void errorCodeCarriesHttpStatusAndDefaultMessage() {
        assertEquals(HttpStatus.NOT_FOUND, ErrorCode.CAMPAIGN_NOT_FOUND.getHttpStatus());
        assertEquals("Campaign not found.", ErrorCode.CAMPAIGN_NOT_FOUND.getDefaultMessage());
    }

    @Test
    void businessExceptionUsesDefaultMessageWhenCustomMessageIsNotProvided() {
        BusinessException exception = new BusinessException(ErrorCode.INVALID_CAMPAIGN_STATUS);

        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS, exception.getErrorCode());
        assertEquals(ErrorCode.INVALID_CAMPAIGN_STATUS.getDefaultMessage(), exception.getMessage());
    }
}

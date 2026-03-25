package com.listen.portfolio.common.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    BAD_REQUEST("400", "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("401", "Unauthorized", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("403", "Forbidden", HttpStatus.FORBIDDEN),
    INTERNAL_ERROR("1", "Internal server error", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String messageId;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String messageId, String defaultMessage, HttpStatus httpStatus) {
        this.messageId = messageId;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getMessageId() {
        return messageId;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}


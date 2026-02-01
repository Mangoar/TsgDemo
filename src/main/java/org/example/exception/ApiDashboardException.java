package org.example.exception;

import io.netty.handler.codec.http.HttpResponseStatus;

public class ApiDashboardException extends RuntimeException {
    private final HttpResponseStatus status;

    public ApiDashboardException(HttpResponseStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpResponseStatus getStatus() {
        return status;
    }
}

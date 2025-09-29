package com.example.rest;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class GlobalExceptionMapper implements ExceptionMapper<RuntimeException> {
    @Override
    public Response toResponse(RuntimeException e) {
        return Response.status(Response.Status.BAD_GATEWAY)
                .entity(new ErrorPayload("external_api_error", e.getMessage()))
                .build();
    }

    public static class ErrorPayload {
        public String code;
        public String message;
        public ErrorPayload(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}

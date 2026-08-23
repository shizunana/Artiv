package com.artiv.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void noPermissionUsesRealHttp403AndKeepsBusinessCode() {
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new BusinessException(ResultCode.NO_PERMISSION));

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals(ResultCode.NO_PERMISSION.getCode(), response.getBody().getCode());
    }

    @Test
    void ordinaryBusinessValidationKeepsHttp200Compatibility() {
        ResponseEntity<Result<Void>> response = handler.handleBusiness(
                new BusinessException(ResultCode.PARAM_INVALID));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(ResultCode.PARAM_INVALID.getCode(), response.getBody().getCode());
    }
}

package ecom.user_service.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import ecom.user_service.dto.response.ErrorResponse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateKeyRace_ReturnsConflict() {
        ResponseEntity<ErrorResponse> response = handler.handleDuplicateKey(
                new DuplicateKeyException("duplicate index"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Email or username already exists", response.getBody().message());
    }

    @Test
    void badRequest_PreservesSafeMessage() {
        ResponseEntity<ErrorResponse> response = handler.handleBadRequestException(
                new BadRequestException("avatarMediaId is invalid"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("avatarMediaId is invalid", response.getBody().message());
    }
}

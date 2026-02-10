package ecom.user_service.dto.response;

import java.util.Map;

public record ErrorResponse(
        int status,
        String message,
        Map<String, String> errors) {

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}

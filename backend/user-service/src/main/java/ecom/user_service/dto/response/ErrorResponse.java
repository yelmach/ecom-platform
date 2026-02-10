package ecom.user_service.dto.response;

public record ErrorResponse(
        int status,
        String message,
        Object dtails) {

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}

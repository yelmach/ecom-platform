package ecom.user_service.dto.response;

public record ErrorResponse(
        int status,
        String message,
        Object details) {

    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}

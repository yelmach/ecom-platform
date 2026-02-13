package ecom.media_service.service;

public record ValidatedFile(
        byte[] bytes,
        String contentType,
        String extension) {
}


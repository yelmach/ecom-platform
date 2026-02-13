package ecom.media_service.service;

import org.springframework.stereotype.Component;

import ecom.media_service.config.MediaProperties;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MediaUrlBuilder {

    private final MediaProperties mediaProperties;

    public String build(String objectKey) {
        String baseUrl = trimTrailingSlash(mediaProperties.getPublicBaseUrl());
        String normalizedKey = trimLeadingSlash(objectKey);
        return baseUrl + "/" + normalizedKey;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}


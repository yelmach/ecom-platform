package ecom.product_service.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaValidationService {

    private final RestClient.Builder restClientBuilder;

    @Value("${media.service.base-url}")
    private String mediaServiceBaseUrl;

    public void validateProductMediaReferences(String productId, List<String> mediaIds) {
        if (mediaIds == null || mediaIds.isEmpty()) {
            return;
        }

        Set<String> validMediaIds = fetchMediaIdsForProduct(productId);
        List<String> invalidIds = mediaIds.stream()
                .filter(mediaId -> !validMediaIds.contains(mediaId))
                .toList();

        if (!invalidIds.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid mediaIds for product: " + String.join(", ", invalidIds));
        }
    }

    private Set<String> fetchMediaIdsForProduct(String productId) {
        try {
            ProductImagesResponse response = restClientBuilder.build()
                    .get()
                    .uri(buildUrl("/media/images/{productId}"), productId)
                    .retrieve()
                    .body(ProductImagesResponse.class);

            if (response == null || response.images() == null) {
                return Set.of();
            }

            return response.images().stream()
                    .map(MediaImageResponse::id)
                    .collect(Collectors.toSet());
        } catch (RestClientException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to validate media references",
                    ex);
        }
    }

    private String buildUrl(String path) {
        if (mediaServiceBaseUrl.endsWith("/")) {
            return mediaServiceBaseUrl.substring(0, mediaServiceBaseUrl.length() - 1) + path;
        }
        return mediaServiceBaseUrl + path;
    }

    private record ProductImagesResponse(String productId, List<MediaImageResponse> images) {
    }

    private record MediaImageResponse(String id, String url) {
    }
}

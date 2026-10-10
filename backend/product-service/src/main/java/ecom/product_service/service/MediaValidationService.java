package ecom.product_service.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaValidationService {

    private final RestClient mediaServiceRestClient;

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
            ProductImagesResponse response = mediaServiceRestClient
                    .get()
                    .uri("/media/images/{productId}", productId)
                    .retrieve()
                    .body(ProductImagesResponse.class);

            if (response == null || response.images() == null) {
                return Set.of();
            }

            return response.images().stream()
                    .map(MediaImageResponse::id)
                    .collect(Collectors.toSet());
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to validate media references",
                    ex);
        }
    }

    private record ProductImagesResponse(String productId, List<MediaImageResponse> images) {
    }

    private record MediaImageResponse(String id, String url) {
    }
}

package ecom.media_service.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductOwnershipService {
    private final RestClient productServiceRestClient;

    public void verifyOwnership(String productId, String userId) {
        try {
            productServiceRestClient.get()
                    .uri("/internal/products/{productId}/ownership", productId)
                    .header("X-User-Id", userId)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 204) {
                            return null;
                        }
                        if (status == 403) {
                            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                                    "You can only upload images for your own products");
                        }
                        if (status == 404) {
                            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found");
                        }
                        if (status == 400) {
                            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ownership request");
                        }
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Product ownership verification is unavailable");
                    });
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Product ownership verification is unavailable", ex);
        }
    }
}

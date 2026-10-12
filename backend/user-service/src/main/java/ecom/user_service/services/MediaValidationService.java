package ecom.user_service.services;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import ecom.user_service.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaValidationService {
    private final RestClient mediaServiceRestClient;

    public void validateAvatarOwnership(String userId, String avatarMediaId) {
        try {
            mediaServiceRestClient.get()
                    .uri("/internal/media/avatars/{mediaId}/ownership", avatarMediaId)
                    .header("X-User-Id", userId)
                    .exchange((request, response) -> {
                        int status = response.getStatusCode().value();
                        if (status == 204) {
                            return null;
                        }
                        if (status == 403) {
                            throw new BadRequestException("avatarMediaId does not belong to the current user");
                        }
                        if (status == 400 || status == 404) {
                            throw new BadRequestException("avatarMediaId is invalid");
                        }
                        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                                "Failed to validate avatar media reference");
                    });
        } catch (RestClientException | IllegalStateException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Failed to validate avatar media reference", ex);
        }
    }

}

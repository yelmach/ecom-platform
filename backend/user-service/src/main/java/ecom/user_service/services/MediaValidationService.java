package ecom.user_service.services;

import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ecom.user_service.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaValidationService {

    private final RestClient mediaServiceRestClient;

    public void validateAvatarOwnership(String userId, String avatarMediaId) {
        try {
            ProfileImageResponse response = mediaServiceRestClient
                    .get()
                    .uri("/media/profile/{userId}", userId)
                    .retrieve()
                    .body(ProfileImageResponse.class);

            if (response == null || response.avatar() == null || response.avatar().id() == null) {
                throw new BadRequestException("avatarMediaId is invalid");
            }

            if (!avatarMediaId.equals(response.avatar().id())) {
                throw new BadRequestException("avatarMediaId does not belong to the current user");
            }
        } catch (HttpClientErrorException.NotFound ex) {
            throw new BadRequestException("avatarMediaId is invalid");
        } catch (RestClientException ex) {
            throw new IllegalStateException("Failed to validate avatar media reference", ex);
        }
    }

    private record ProfileImageResponse(MediaImageResponse avatar) {
    }

    private record MediaImageResponse(String id, String url) {
    }
}

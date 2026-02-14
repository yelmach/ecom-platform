package ecom.user_service.services;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import ecom.user_service.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaValidationService {

    private final RestClient.Builder restClientBuilder;

    @Value("${media.service.base-url}")
    private String mediaServiceBaseUrl;

    public void validateAvatarOwnership(String userId, String avatarMediaId) {
        try {
            ProfileImageResponse response = restClientBuilder.build()
                    .get()
                    .uri(buildUrl("/media/profile/{userId}"), userId)
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

    private String buildUrl(String path) {
        if (mediaServiceBaseUrl.endsWith("/")) {
            return mediaServiceBaseUrl.substring(0, mediaServiceBaseUrl.length() - 1) + path;
        }
        return mediaServiceBaseUrl + path;
    }

    private record ProfileImageResponse(MediaImageResponse avatar) {
    }

    private record MediaImageResponse(String id, String url) {
    }
}

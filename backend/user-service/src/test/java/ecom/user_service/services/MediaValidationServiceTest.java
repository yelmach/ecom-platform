package ecom.user_service.services;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import ecom.user_service.exceptions.BadRequestException;

class MediaValidationServiceTest {

    private MediaValidationService mediaValidationService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        mockServer = MockRestServiceServer.bindTo(builder).build();
        mediaValidationService = new MediaValidationService(builder);
        ReflectionTestUtils.setField(mediaValidationService, "mediaServiceBaseUrl", "http://media-service");
    }

    @Test
    void validateAvatarOwnership_ShouldPassWhenAvatarBelongsToUser() {
        mockServer.expect(requestTo("http://media-service/media/profile/user-123"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"avatar\":{\"id\":\"media-123\",\"url\":\"https://cdn/media-123.png\"}}",
                        MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> mediaValidationService.validateAvatarOwnership("user-123", "media-123"));
        mockServer.verify();
    }

    @Test
    void validateAvatarOwnership_ShouldThrowWhenAvatarDoesNotBelongToUser() {
        mockServer.expect(requestTo("http://media-service/media/profile/user-123"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"avatar\":{\"id\":\"media-999\",\"url\":\"https://cdn/media-999.png\"}}",
                        MediaType.APPLICATION_JSON));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> mediaValidationService.validateAvatarOwnership("user-123", "media-123"));

        assertEquals("avatarMediaId does not belong to the current user", ex.getMessage());
    }

    @Test
    void validateAvatarOwnership_ShouldThrowWhenAvatarResponseIsInvalid() {
        mockServer.expect(requestTo("http://media-service/media/profile/user-123"))
                .andExpect(method(GET))
                .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> mediaValidationService.validateAvatarOwnership("user-123", "media-123"));

        assertEquals("avatarMediaId is invalid", ex.getMessage());
    }

    @Test
    void validateAvatarOwnership_ShouldThrowBadRequestOnNotFound() {
        mockServer.expect(requestTo("http://media-service/media/profile/user-123"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> mediaValidationService.validateAvatarOwnership("user-123", "media-123"));

        assertEquals("avatarMediaId is invalid", ex.getMessage());
    }

    @Test
    void validateAvatarOwnership_ShouldThrowIllegalStateOnUnexpectedRestError() {
        mockServer.expect(requestTo("http://media-service/media/profile/user-123"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> mediaValidationService.validateAvatarOwnership("user-123", "media-123"));

        assertEquals("Failed to validate avatar media reference", ex.getMessage());
        assertInstanceOf(org.springframework.web.client.RestClientException.class, ex.getCause());
    }
}

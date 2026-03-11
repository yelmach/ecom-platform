package ecom.product_service.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

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
    void validateProductMediaReferences_ShouldPassWhenIdsAreValid() {
        mockServer.expect(requestTo("http://media-service/media/images/prod-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"productId\":\"prod-1\",\"images\":[{\"id\":\"m1\",\"url\":\"u1\"},{\"id\":\"m2\",\"url\":\"u2\"}]}",
                        MediaType.APPLICATION_JSON));

        assertDoesNotThrow(() -> mediaValidationService.validateProductMediaReferences("prod-1", List.of("m1", "m2")));
        mockServer.verify();
    }

    @Test
    void validateProductMediaReferences_ShouldThrowWhenContainsInvalidIds() {
        mockServer.expect(requestTo("http://media-service/media/images/prod-1"))
                .andExpect(method(GET))
                .andRespond(withSuccess(
                        "{\"productId\":\"prod-1\",\"images\":[{\"id\":\"m1\",\"url\":\"u1\"}]}",
                        MediaType.APPLICATION_JSON));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> mediaValidationService.validateProductMediaReferences("prod-1", List.of("m1", "m9")));

        assertEquals(400, ex.getStatusCode().value());
        assertEquals("Invalid mediaIds for product: m9", ex.getReason());
    }

    @Test
    void validateProductMediaReferences_ShouldIgnoreNullOrEmptyInput() {
        assertDoesNotThrow(() -> mediaValidationService.validateProductMediaReferences("prod-1", null));
        assertDoesNotThrow(() -> mediaValidationService.validateProductMediaReferences("prod-1", List.of()));
    }

    @Test
    void validateProductMediaReferences_ShouldThrowServiceUnavailableOnRemoteFailure() {
        mockServer.expect(requestTo("http://media-service/media/images/prod-1"))
                .andExpect(method(GET))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> mediaValidationService.validateProductMediaReferences("prod-1", List.of("m1")));

        assertEquals(503, ex.getStatusCode().value());
        assertEquals("Failed to validate media references", ex.getReason());
    }
}

package ecom.user_service.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.client.MockRestServiceServer;
import ecom.user_service.exceptions.BadRequestException;

class MediaValidationServiceTest {
    private MediaValidationService service;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        var builder = RestClient.builder().baseUrl("http://media-service");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new MediaValidationService(builder.build());
    }

    @Test
    void acceptsOwnedAvatarAndForwardsIdentity() {
        server.expect(requestTo("http://media-service/internal/media/avatars/avatar-1/ownership"))
                .andExpect(header("X-User-Id", "user-1"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        assertDoesNotThrow(() -> service.validateAvatarOwnership("user-1", "avatar-1"));
        server.verify();
    }

    @Test
    void wrongOwnerAndMissingAvatarAreBadRequests() {
        for (HttpStatus status : new HttpStatus[] {HttpStatus.FORBIDDEN, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST}) {
            server.reset();
            server.expect(anything()).andRespond(withStatus(status));
            assertThrows(BadRequestException.class, () -> service.validateAvatarOwnership("user-1", "avatar-1"));
            server.verify();
        }
    }

    @Test
    void remoteAndUnexpectedResponsesAreServiceUnavailable() {
        for (HttpStatus status : new HttpStatus[] {HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.OK}) {
            server.reset();
            server.expect(anything()).andRespond(withStatus(status));
            var ex = assertThrows(ResponseStatusException.class,
                    () -> service.validateAvatarOwnership("user-1", "avatar-1"));
            assertEquals(503, ex.getStatusCode().value());
        }
    }

    @Test
    void connectionFailureIsServiceUnavailable() {
        server.expect(anything()).andRespond(withException(new IOException("connection refused")));
        assertEquals(503, assertThrows(ResponseStatusException.class,
                () -> service.validateAvatarOwnership("user-1", "avatar-1")).getStatusCode().value());
    }

}

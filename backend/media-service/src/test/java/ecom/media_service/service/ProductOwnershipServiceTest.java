package ecom.media_service.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

import java.net.SocketTimeoutException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class ProductOwnershipServiceTest {
    private MockRestServiceServer server;
    private ProductOwnershipService service;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://product-service");
        server = MockRestServiceServer.bindTo(builder).build();
        service = new ProductOwnershipService(builder.build());
    }

    @Test
    void callsInternalEndpointOnceWithTrustedIdentity() {
        server.expect(requestTo("http://product-service/internal/products/prod-1/ownership"))
                .andExpect(method(HttpMethod.GET)).andExpect(header("X-User-Id", "seller-1"))
                .andRespond(withStatus(HttpStatus.NO_CONTENT));
        service.verifyOwnership("prod-1", "seller-1");
        server.verify();
    }

    @Test
    void preservesOwnershipErrorsAndRejectsUnexpectedResponses() {
        for (HttpStatus status : new HttpStatus[] { HttpStatus.BAD_REQUEST, HttpStatus.FORBIDDEN,
                HttpStatus.NOT_FOUND, HttpStatus.INTERNAL_SERVER_ERROR, HttpStatus.OK }) {
            server.reset();
            server.expect(requestTo("http://product-service/internal/products/prod-1/ownership"))
                    .andRespond(withStatus(status));
            ResponseStatusException failure = assertThrows(ResponseStatusException.class,
                    () -> service.verifyOwnership("prod-1", "seller-1"));
            HttpStatus expected = status.is4xxClientError() ? status : HttpStatus.SERVICE_UNAVAILABLE;
            assertEquals(expected, failure.getStatusCode());
            server.verify();
        }
    }

    @Test
    void connectionAndTimeoutFailuresReceive503() {
        for (ResourceAccessException failure : new ResourceAccessException[] {
                new ResourceAccessException("connection refused"),
                new ResourceAccessException("response timeout", new SocketTimeoutException()) }) {
            server.reset();
            server.expect(requestTo("http://product-service/internal/products/prod-1/ownership"))
                    .andRespond(request -> { throw failure; });
            assertEquals(HttpStatus.SERVICE_UNAVAILABLE, assertThrows(ResponseStatusException.class,
                    () -> service.verifyOwnership("prod-1", "seller-1")).getStatusCode());
            server.verify();
        }
    }
}

package ecom.media_service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import ecom.media_service.service.MediaCleanupService;
import tools.jackson.databind.json.JsonMapper;

class ProductEventConsumerTest {
    private final MediaCleanupService cleanup = mock(MediaCleanupService.class);
    private final ProductEventConsumer listener = new ProductEventConsumer(JsonMapper.builder().build(), cleanup);
    private final String event = """
            {"eventId":"00000000-0000-4000-8000-000000000001","eventType":"PRODUCT_DELETED",
             "occurredAt":"2026-10-11T12:00:00Z","payload":{"productId":"p"}}
            """;

    private ConsumerRecord<String, String> record(String key, String value) {
        return new ConsumerRecord<>("product.events", 0, 0, key, value);
    }

    @Test
    void validDeletionCallsCleanup() {
        listener.onProductEvent(record("p", event));
        verify(cleanup).handleProductDeleted("p");
    }

    @Test
    void invalidEventsCannotDeleteMedia() {
        for (String invalid : new String[] { "not json", event.replace("2026-10-11T12:00:00Z", "invalid timestamp"),
                event.replace("\"productId\":\"p\"", "\"productId\":\"\"") }) {
            assertThrows(IllegalArgumentException.class, () -> listener.onProductEvent(record("p", invalid)));
        }
        assertThrows(IllegalArgumentException.class, () -> listener.onProductEvent(record("other", event)));
        verifyNoInteractions(cleanup);
    }

    @Test
    void blankProductIdIsRejectedEvenWhenKeyMatches() {
        for (String productId : new String[] { "", "   " }) {
            String invalid = event.replace("\"productId\":\"p\"", "\"productId\":\"" + productId + "\"");
            assertThrows(IllegalArgumentException.class,
                    () -> listener.onProductEvent(record(productId, invalid)));
        }
        verifyNoInteractions(cleanup);
    }

    @Test
    void otherEventTypesAreIgnored() {
        listener.onProductEvent(record("p", event.replace("PRODUCT_DELETED", "PRODUCT_IMAGES_DETACHED")));
        verifyNoInteractions(cleanup);
    }

    @Test
    void cleanupFailureEscapesForRetry() {
        var failure = new IllegalStateException("storage offline");
        doThrow(failure).when(cleanup).handleProductDeleted("p");
        assertSame(failure, assertThrows(IllegalStateException.class,
                () -> listener.onProductEvent(record("p", event))));
    }
}

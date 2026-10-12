package ecom.product_service.event;

import java.time.Instant;
import java.util.UUID;

public record ProductDeletedEvent(String eventId, String eventType, String occurredAt, Payload payload) {

    public static final String TOPIC = "product.events";

    public static ProductDeletedEvent forProduct(String productId) {
        return new ProductDeletedEvent(UUID.randomUUID().toString(), "PRODUCT_DELETED",
                Instant.now().toString(), new Payload(productId));
    }

    public record Payload(String productId) {
    }
}

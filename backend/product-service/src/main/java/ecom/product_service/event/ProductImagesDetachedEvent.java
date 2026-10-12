package ecom.product_service.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductImagesDetachedEvent(String eventId, String eventType, String occurredAt, Payload payload) {

    public static final String TOPIC = "product.events";

    public static ProductImagesDetachedEvent forProduct(String productId, List<String> mediaIds) {
        return new ProductImagesDetachedEvent(UUID.randomUUID().toString(), "PRODUCT_IMAGES_DETACHED",
                Instant.now().toString(), new Payload(productId, List.copyOf(mediaIds)));
    }

    public record Payload(String productId, List<String> mediaIds) {
    }
}

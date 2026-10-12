package ecom.media_service.event;

import java.time.Instant;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import ecom.media_service.service.MediaCleanupService;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ProductEventConsumer {

    private final JsonMapper jsonMapper;
    private final MediaCleanupService cleanupService;

    @KafkaListener(topics = "product.events", groupId = "media-product-cleanup")
    public void onProductEvent(ConsumerRecord<String, String> record) {

        String productId;
        String eventType;
        List<String> mediaIds = new ArrayList<>();

        try {
            JsonNode event = jsonMapper.readTree(record.value());
            if (!event.isObject()) {
                throw new IllegalArgumentException("Event must be an object");
            }

            eventType = event.path("eventType").asString();
            if (!"PRODUCT_DELETED".equals(eventType) && !"PRODUCT_IMAGES_DETACHED".equals(eventType)) {
                return;
            }

            UUID.fromString(event.path("eventId").asString());
            Instant.parse(event.path("occurredAt").asString());
            productId = event.path("payload").path("productId").asString();

            if (productId.isBlank()) {
                throw new IllegalArgumentException("productId must not be blank");
            }
            if (!productId.equals(record.key())) {
                throw new IllegalArgumentException("Product event key does not match payload");
            }
            if ("PRODUCT_IMAGES_DETACHED".equals(eventType)) {
                JsonNode images = event.path("payload").path("mediaIds");
                if (!images.isArray() || images.isEmpty()) {
                    throw new IllegalArgumentException("mediaIds must be a non-empty array");
                }
                for (JsonNode image : images) {
                    if (!image.isString() || image.asString().isBlank()) {
                        throw new IllegalArgumentException("mediaIds must contain non-blank strings");
                    }
                    mediaIds.add(image.asString());
                }
            }
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid product cleanup event", ex);
        }
        if ("PRODUCT_DELETED".equals(eventType)) {
            cleanupService.handleProductDeleted(productId);
        } else {
            cleanupService.handleProductImagesDetached(productId, mediaIds);
        }
    }
}

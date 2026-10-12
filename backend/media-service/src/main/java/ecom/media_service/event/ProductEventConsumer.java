package ecom.media_service.event;

import java.time.Instant;
import java.util.UUID;

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

        try {
            JsonNode event = jsonMapper.readTree(record.value());
            if (!event.isObject()) {
                throw new IllegalArgumentException("Event must be an object");
            }

            String eventType = event.path("eventType").asString();
            if (!"PRODUCT_DELETED".equals(eventType)) {
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
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid product deletion event", ex);
        }
        cleanupService.handleProductDeleted(productId);
    }
}

package ecom.product_service.event;

import java.util.concurrent.TimeUnit;
import java.util.List;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProductEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publishProductDeleted(String productId) {
        try {
            ProductDeletedEvent event = ProductDeletedEvent.forProduct(productId);
            kafkaTemplate.send(ProductDeletedEvent.TOPIC, productId, jsonMapper.writeValueAsString(event))
                    .get(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logPublishingFailure(productId, ex);
        } catch (Exception ex) {
            logPublishingFailure(productId, ex);
        }
    }

    private void logPublishingFailure(String productId, Exception failure) {
        log.error("Product {} was deleted but its media cleanup event could not be published", productId, failure);
    }

    public void publishProductImagesDetached(String productId, List<String> mediaIds) {
        try {
            ProductImagesDetachedEvent event = ProductImagesDetachedEvent.forProduct(productId, mediaIds);
            kafkaTemplate.send(ProductImagesDetachedEvent.TOPIC, productId, jsonMapper.writeValueAsString(event))
                    .get(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logDetachedImagesFailure(productId, ex);
        } catch (Exception ex) {
            logDetachedImagesFailure(productId, ex);
        }
    }

    private void logDetachedImagesFailure(String productId, Exception failure) {
        log.error("Product {} was updated but its detached-image cleanup event could not be published", productId,
                failure);
    }
}

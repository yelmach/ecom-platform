package ecom.product_service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

class ProductEventProducerTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final ProductEventProducer publisher = new ProductEventProducer(kafka, mapper);

    @Test
    void publishesDeletionDirectlyWithProductIdAsKey() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.completedFuture(null));
        publisher.publishProductDeleted("p");
        var message = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(kafka).send(eq(ProductDeletedEvent.TOPIC), eq("p"), message.capture());
        var event = mapper.readValue(message.getValue(), ProductDeletedEvent.class);
        assertEquals("PRODUCT_DELETED", event.eventType());
        assertEquals("p", event.payload().productId());
        assertFalse(mapper.readTree(message.getValue()).has("schemaVersion"));
        assertNotNull(event.eventId());
    }

    @Test
    void failedPublicationDoesNotFailProductDeletion() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker offline")));
        assertDoesNotThrow(() -> publisher.publishProductDeleted("p"));
        verify(kafka, times(1)).send(anyString(), eq("p"), anyString());
    }

    @Test
    void immediateSendFailureDoesNotFailProductDeletion() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenThrow(new IllegalStateException("producer unavailable"));
        assertDoesNotThrow(() -> publisher.publishProductDeleted("p"));
    }
}

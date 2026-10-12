package ecom.user_service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import tools.jackson.databind.json.JsonMapper;

class UserEventProducerTest {
    @SuppressWarnings("unchecked")
    private final KafkaTemplate<String, String> kafka = mock(KafkaTemplate.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final UserEventProducer producer = new UserEventProducer(kafka, mapper);

    @Test
    void replacementAndClearingPublishUserKeyAndAvatarIds() {
        when(kafka.send(anyString(), anyString(), anyString())).thenReturn(CompletableFuture.completedFuture(null));
        producer.publishUserAvatarChanged("u", "old", "new");
        producer.publishUserAvatarChanged("u", "old", null);
        var messages = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(kafka, times(2)).send(eq("user.events"), eq("u"), messages.capture());
        var replacement = mapper.readValue(messages.getAllValues().get(0), UserAvatarChangedEvent.class);
        var clearing = mapper.readValue(messages.getAllValues().get(1), UserAvatarChangedEvent.class);
        assertEquals("USER_AVATAR_CHANGED", replacement.eventType());
        assertEquals("u", replacement.payload().userId());
        assertEquals("old", replacement.payload().previousMediaId());
        assertEquals("new", replacement.payload().currentMediaId());
        assertNull(clearing.payload().currentMediaId());
        assertTrue(mapper.readTree(messages.getAllValues().get(1)).path("payload").path("currentMediaId").isNull());
    }

    @Test
    void publicationFailureDoesNotFailProfileUpdate() {
        when(kafka.send(anyString(), anyString(), anyString()))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker offline")));
        assertDoesNotThrow(() -> producer.publishUserAvatarChanged("u", "old", "new"));
        when(kafka.send(anyString(), anyString(), anyString())).thenThrow(new IllegalStateException("producer offline"));
        assertDoesNotThrow(() -> producer.publishUserAvatarChanged("u", "old", null));
    }
}

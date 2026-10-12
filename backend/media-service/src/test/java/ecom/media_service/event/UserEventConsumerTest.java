package ecom.media_service.event;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import ecom.media_service.service.MediaCleanupService;
import tools.jackson.databind.json.JsonMapper;

class UserEventConsumerTest {
    private final MediaCleanupService cleanup = mock(MediaCleanupService.class);
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final UserEventConsumer consumer = new UserEventConsumer(mapper, cleanup);

    private ConsumerRecord<String, String> event(String key, String previous, String current) {
        var root = mapper.createObjectNode();
        root.put("eventId", "00000000-0000-4000-8000-000000000001");
        root.put("eventType", "USER_AVATAR_CHANGED");
        root.put("occurredAt", "2026-10-12T12:00:00Z");
        var payload = root.putObject("payload");
        payload.put("userId", "u");
        payload.put("previousMediaId", previous);
        payload.put("currentMediaId", current);
        return new ConsumerRecord<>("user.events", 0, 0, key, mapper.writeValueAsString(root));
    }

    @Test
    void replacementAndClearingCleanPreviousAvatar() {
        consumer.onUserEvent(event("u", "old", "new"));
        consumer.onUserEvent(event("u", "old", null));
        verify(cleanup, times(2)).handleUserAvatarChanged("u", "old");
    }

    @Test
    void firstSelectionAndUnchangedAvatarDoNotCleanUp() {
        consumer.onUserEvent(event("u", null, "new"));
        consumer.onUserEvent(event("u", "old", "old"));
        verifyNoInteractions(cleanup);
    }

    @Test
    void invalidEventsNeverReachCleanup() {
        assertThrows(IllegalArgumentException.class, () -> consumer.onUserEvent(event("other", "old", "new")));
        assertThrows(IllegalArgumentException.class, () -> consumer.onUserEvent(event("u", "", "new")));
        assertThrows(IllegalArgumentException.class, () -> consumer.onUserEvent(event("u", "old", " ")));
        assertThrows(IllegalArgumentException.class, () -> consumer.onUserEvent(
                new ConsumerRecord<>("user.events", 0, 0, "u", "not json")));
        verifyNoInteractions(cleanup);
    }

    @Test
    void cleanupFailureEscapesForRetry() {
        var failure = new IllegalStateException("storage offline");
        doThrow(failure).when(cleanup).handleUserAvatarChanged("u", "old");
        assertSame(failure, assertThrows(IllegalStateException.class, () -> consumer.onUserEvent(event("u", "old", "new"))));
    }
}

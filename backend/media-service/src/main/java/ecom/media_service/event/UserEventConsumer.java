package ecom.media_service.event;

import java.time.Instant;
import java.util.Objects;
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
public class UserEventConsumer {
    private final JsonMapper jsonMapper;
    private final MediaCleanupService cleanupService;

    @KafkaListener(topics = "user.events", groupId = "media-avatar-cleanup")
    public void onUserEvent(ConsumerRecord<String, String> record) {
        String userId;
        String previousMediaId;
        String currentMediaId;
        try {
            JsonNode event = jsonMapper.readTree(record.value());
            if (!event.isObject()) {
                throw new IllegalArgumentException("Event must be an object");
            }
            if (!"USER_AVATAR_CHANGED".equals(event.path("eventType").asString())) {
                return;
            }
            UUID.fromString(event.path("eventId").asString());
            Instant.parse(event.path("occurredAt").asString());
            JsonNode payload = event.path("payload");
            userId = payload.path("userId").asString();
            if (userId.isBlank() || !userId.equals(record.key())) {
                throw new IllegalArgumentException("Invalid user ID or event key");
            }
            previousMediaId = nullableMediaId(payload.path("previousMediaId"));
            currentMediaId = nullableMediaId(payload.path("currentMediaId"));
        } catch (RuntimeException ex) {
            throw new IllegalArgumentException("Invalid avatar cleanup event", ex);
        }
        if (previousMediaId != null && !Objects.equals(previousMediaId, currentMediaId)) {
            cleanupService.handleUserAvatarChanged(userId, previousMediaId);
        }
    }

    private String nullableMediaId(JsonNode node) {
        if (node.isNull()) {
            return null;
        }
        if (!node.isString() || node.asString().isBlank()) {
            throw new IllegalArgumentException("Avatar IDs must be non-blank strings or null");
        }
        return node.asString();
    }
}

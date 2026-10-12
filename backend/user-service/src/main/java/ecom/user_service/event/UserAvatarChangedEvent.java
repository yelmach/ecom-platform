package ecom.user_service.event;

import java.time.Instant;
import java.util.UUID;

public record UserAvatarChangedEvent(String eventId, String eventType, String occurredAt, Payload payload) {

    public static final String TOPIC = "user.events";

    public static UserAvatarChangedEvent forUser(String userId, String previousMediaId, String currentMediaId) {
        return new UserAvatarChangedEvent(UUID.randomUUID().toString(), "USER_AVATAR_CHANGED",
                Instant.now().toString(), new Payload(userId, previousMediaId, currentMediaId));
    }

    public record Payload(String userId, String previousMediaId, String currentMediaId) {
    }
}

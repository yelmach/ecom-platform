package ecom.user_service.event;

import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final JsonMapper jsonMapper;

    public void publishUserAvatarChanged(String userId, String previousMediaId, String currentMediaId) {
        try {
            UserAvatarChangedEvent event = UserAvatarChangedEvent.forUser(userId, previousMediaId, currentMediaId);
            kafkaTemplate.send(UserAvatarChangedEvent.TOPIC, userId, jsonMapper.writeValueAsString(event))
                    .get(15, TimeUnit.SECONDS);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            logPublishingFailure(userId, ex);
        } catch (Exception ex) {
            logPublishingFailure(userId, ex);
        }
    }

    private void logPublishingFailure(String userId, Exception failure) {
        log.error("User {} was updated but its avatar cleanup event could not be published", userId, failure);
    }
}

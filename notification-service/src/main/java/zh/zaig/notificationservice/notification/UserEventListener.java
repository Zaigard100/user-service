package zh.zaig.notificationservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {

    private static final Logger log = LoggerFactory.getLogger(UserEventListener.class);

    private final NotificationService notificationService;

    public UserEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "${app.kafka.user-events-topic}")
    public void onUserEvent(UserEvent event) {
        log.debug("Получено событие из Kafka: {}", event);
        notificationService.notify(event);
    }
}

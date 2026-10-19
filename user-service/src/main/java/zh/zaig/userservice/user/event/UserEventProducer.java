package zh.zaig.userservice.user.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class UserEventProducer {

    private static final Logger log = LoggerFactory.getLogger(UserEventProducer.class);

    private final KafkaTemplate<String, UserEvent> kafkaTemplate;
    private final String topic;

    public UserEventProducer(KafkaTemplate<String, UserEvent> kafkaTemplate,
                             @Value("${app.kafka.user-events-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @TransactionalEventListener
    public void send(UserEvent event) {
        kafkaTemplate.send(topic, event.email(), event).whenComplete((result, e) -> {
            if (e != null) {
                log.error("Не удалось отправить событие {} в Kafka", event, e);
            } else {
                log.debug("Событие {} отправлено в {}", event, result.getRecordMetadata());
            }
        });
    }
}

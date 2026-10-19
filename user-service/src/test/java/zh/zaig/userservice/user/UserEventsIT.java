package zh.zaig.userservice.user;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.kafka.KafkaContainer;
import zh.zaig.userservice.KafkaTestcontainersConfiguration;
import zh.zaig.userservice.TestcontainersConfiguration;
import zh.zaig.userservice.user.event.UserEvent;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, KafkaTestcontainersConfiguration.class})
@DisplayName("События пользователей в Kafka (интеграционные тесты)")
class UserEventsIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KafkaContainer kafka;

    @Value("${app.kafka.user-events-topic}")
    private String topic;

    private Consumer<String, UserEvent> consumer;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

        Map<String, Object> props = Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, "test-" + UUID.randomUUID(),
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"
        );
        consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new JsonDeserializer<>(UserEvent.class, false)).createConsumer();
        consumer.subscribe(List.of(topic));
    }

    @AfterEach
    void tearDown() {
        consumer.close();
    }

    @Test
    @DisplayName("POST отправляет событие CREATED")
    void create_validRequest_sendsCreatedEvent() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Сатор Арепыч", "email": "arepych@mail.com", "age": 54}
                                """))
                .andExpect(status().isCreated());

        assertEquals(List.of(new UserEvent(UserEvent.Operation.CREATED, "arepych@mail.com")),
                pollEvents("arepych@mail.com", 1));
    }

    @Test
    @DisplayName("DELETE отправляет событие DELETED")
    void delete_existingUser_sendsDeletedEvent() throws Exception {
        User user = userRepository.save(new User("Ризванов Диаметр", "diametr@mail.com", 19));

        mockMvc.perform(delete("/api/users/" + user.getId()))
                .andExpect(status().isNoContent());

        assertEquals(List.of(new UserEvent(UserEvent.Operation.DELETED, "diametr@mail.com")),
                pollEvents("diametr@mail.com", 1));
    }

    @Test
    @DisplayName("POST с занятым email не отправляет событие")
    void create_duplicateEmail_sendsNoEvent() throws Exception {
        userRepository.save(new User("Шварев Нейрослоп", "neiroslop@mail.com", 22));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Шварев Нейрослоп", "email": "neiroslop@mail.com", "age": 22}
                                """))
                .andExpect(status().isConflict());

        assertEquals(List.of(), pollEvents("neiroslop@mail.com", 1));
    }

    private List<UserEvent> pollEvents(String email, int expected) {
        List<UserEvent> events = new ArrayList<>();
        long deadline = System.currentTimeMillis() + 10_000;
        while (events.size() < expected && System.currentTimeMillis() < deadline) {
            for (ConsumerRecord<String, UserEvent> record : consumer.poll(Duration.ofMillis(200))) {
                if (email.equals(record.key())) {
                    events.add(record.value());
                }
            }
        }
        return events;
    }
}

package zh.zaig.notificationservice.notification;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import zh.zaig.notificationservice.KafkaTestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"spring.mail.host=localhost", "spring.mail.port=3025"})
@Import(KafkaTestcontainersConfiguration.class)
@DisplayName("Отправка писем по событиям из Kafka (интеграционные тесты)")
class UserEventKafkaIT {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.kafka.user-events-topic}")
    private String topic;

    @Test
    @DisplayName("CREATED - письмо о создании аккаунта")
    void createdEvent_sendsAccountCreatedEmail() throws Exception {
        kafkaTemplate.send(topic, "sator@mail.com", """
                {"operation": "CREATED", "email": "sator@mail.com"}
                """);

        MimeMessage message = awaitSingleMessage();
        assertAll(
                () -> assertEquals("sator@mail.com", message.getAllRecipients()[0].toString()),
                () -> assertEquals(NotificationService.SUBJECT, message.getSubject()),
                () -> assertEquals("Здравствуйте! Ваш аккаунт на сайте zaig.zh был успешно создан.",
                        body(message))
        );
    }

    @Test
    @DisplayName("DELETED - письмо об удалении аккаунта")
    void deletedEvent_sendsAccountDeletedEmail() throws Exception {
        kafkaTemplate.send(topic, "rizvanov@mail.com", """
                {"operation": "DELETED", "email": "rizvanov@mail.com"}
                """);

        MimeMessage message = awaitSingleMessage();
        assertAll(
                () -> assertEquals("rizvanov@mail.com", message.getAllRecipients()[0].toString()),
                () -> assertEquals("Здравствуйте! Ваш аккаунт был удалён.", body(message))
        );
    }

    @Test
    @DisplayName("битое сообщение пропускается, следующее обрабатывается")
    void brokenMessage_isSkipped() throws Exception {
        kafkaTemplate.send(topic, "broken", "{not a json");
        kafkaTemplate.send(topic, "shvarev@mail.com", """
                {"operation": "DELETED", "email": "shvarev@mail.com"}
                """);

        MimeMessage message = awaitSingleMessage();
        assertEquals("shvarev@mail.com", message.getAllRecipients()[0].toString());
    }

    private static MimeMessage awaitSingleMessage() {
        assertTrue(greenMail.waitForIncomingEmail(15_000, 1), "Письмо не пришло");
        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        return messages[0];
    }

    private static String body(MimeMessage message) throws Exception {
        return message.getContent().toString().trim();
    }
}

package zh.zaig.notificationservice.notification;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import zh.zaig.notificationservice.KafkaTestcontainersConfiguration;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"spring.mail.host=localhost", "spring.mail.port=3025"})
@AutoConfigureMockMvc
@Import(KafkaTestcontainersConfiguration.class)
@DisplayName("Notification API (интеграционные тесты)")
class NotificationApiIT {

    private static final String URL = "/api/notifications";

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP);

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST с CREATED отправляет письмо о создании аккаунта")
    void send_createdOperation_sendsEmail() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation": "CREATED", "email": "sator@mail.com"}
                                """))
                .andExpect(status().isNoContent());

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertAll(
                () -> assertEquals("sator@mail.com", messages[0].getAllRecipients()[0].toString()),
                () -> assertEquals("Здравствуйте! Ваш аккаунт на сайте zaig.zh был успешно создан.",
                        messages[0].getContent().toString().trim())
        );
    }

    @Test
    @DisplayName("POST с DELETED отправляет письмо об удалении аккаунта")
    void send_deletedOperation_sendsEmail() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation": "DELETED", "email": "rizvanov@mail.com"}
                                """))
                .andExpect(status().isNoContent());

        MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        assertEquals("Здравствуйте! Ваш аккаунт был удалён.", messages[0].getContent().toString().trim());
    }

    @Test
    @DisplayName("POST с некорректным email возвращает 400 и ничего не отправляет")
    void send_invalidEmail_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation": "CREATED", "email": "not-an-email"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, greenMail.getReceivedMessages().length);
    }

    @Test
    @DisplayName("POST с неизвестной операцией возвращает 400 и ничего не отправляет")
    void send_unknownOperation_returnsBadRequest() throws Exception {
        mockMvc.perform(post(URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"operation": "UPDATED", "email": "sator@mail.com"}
                                """))
                .andExpect(status().isBadRequest());

        assertEquals(0, greenMail.getReceivedMessages().length);
    }
}

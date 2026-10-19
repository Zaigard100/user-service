package zh.zaig.notificationservice.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    static final String SUBJECT = "Уведомление об аккаунте";

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final String from;
    private final String siteName;

    public NotificationService(JavaMailSender mailSender,
                               @Value("${app.mail.from}") String from,
                               @Value("${app.mail.site-name}") String siteName) {
        this.mailSender = mailSender;
        this.from = from;
        this.siteName = siteName;
    }

    public void notify(UserEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(event.email());
        message.setSubject(SUBJECT);
        message.setText(text(event.operation()));

        mailSender.send(message);
        log.info("Письмо ({}) отправлено на {}", event.operation(), event.email());
    }

    String text(UserEvent.Operation operation) {
        return switch (operation) {
            case CREATED -> "Здравствуйте! Ваш аккаунт на сайте " + siteName + " был успешно создан.";
            case DELETED -> "Здравствуйте! Ваш аккаунт был удалён.";
        };
    }
}

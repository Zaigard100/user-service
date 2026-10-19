package zh.zaig.notificationservice.notification;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserEvent(

        @NotNull(message = "Операция обязательна")
        Operation operation,

        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный формат email")
        String email
) {

    public enum Operation {
        CREATED,
        DELETED
    }
}

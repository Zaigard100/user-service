package zh.zaig.userservice.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(

        @NotBlank(message = "Имя не может быть пустым")
        @Size(max = 100, message = "Имя не может быть длиннее 100 символов")
        @Schema(example = "Сатор Арепыч")
        String name,

        @NotBlank(message = "Email не может быть пустым")
        @Email(regexp = UserValidation.EMAIL_REGEXP, message = "Некорректный формат email")
        @Size(max = 150, message = "Email не может быть длиннее 150 символов")
        @Schema(example = "sator@mail.com")
        String email,

        @Min(value = 0, message = "Возраст должен быть от 0")
        @Max(value = 150, message = "Возраст должен быть до 150")
        @Schema(example = "54")
        Integer age
) {
}

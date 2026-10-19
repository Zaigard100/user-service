package zh.zaig.userservice.user.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Тело ответа при ошибке. fieldErrors заполняется только при ошибках валидации.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        Map<String, String> fieldErrors,
        LocalDateTime timestamp
) {

    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, null, LocalDateTime.now());
    }

    public static ErrorResponse of(int status, String message, Map<String, String> fieldErrors) {
        return new ErrorResponse(status, message, fieldErrors, LocalDateTime.now());
    }
}

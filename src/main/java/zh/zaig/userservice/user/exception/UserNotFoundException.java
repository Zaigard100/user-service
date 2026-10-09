package zh.zaig.userservice.user.exception;

public class UserNotFoundException extends RuntimeException {

    public UserNotFoundException(Long id) {
        super("Пользователь с id=" + id + " не найден");
    }
}

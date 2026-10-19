package zh.zaig.userservice.user.event;

public record UserEvent(Operation operation, String email) {

    public enum Operation {
        CREATED,
        DELETED
    }
}

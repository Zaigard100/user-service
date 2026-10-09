package zh.zaig.userservice.user.mapper;

import org.springframework.stereotype.Component;
import zh.zaig.userservice.user.User;
import zh.zaig.userservice.user.dto.UserCreateRequest;
import zh.zaig.userservice.user.dto.UserResponse;

@Component
public class UserMapper {

    public User toEntity(UserCreateRequest request) {
        return new User(request.name().trim(), request.email().trim(), request.age());
    }

    public UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getAge(),
                user.getCreatedAt()
        );
    }
}

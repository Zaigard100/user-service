package zh.zaig.userservice.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

@Relation(collectionRelation = "users")
public record UserResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Сатор Арепыч") String name,
        @Schema(example = "sator@mail.com") String email,
        @Schema(example = "54") Integer age,
        LocalDateTime createdAt
) {
}

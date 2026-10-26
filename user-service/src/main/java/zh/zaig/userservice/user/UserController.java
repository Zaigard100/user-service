package zh.zaig.userservice.user;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import zh.zaig.userservice.user.dto.UserCreateRequest;
import zh.zaig.userservice.user.dto.UserResponse;
import zh.zaig.userservice.user.dto.UserUpdateRequest;
import zh.zaig.userservice.user.exception.ErrorResponse;

import java.net.URI;

/**
 * REST API для управления пользователями.
 * <ul>
 *     <li>GET    /api/users       - список всех</li>
 *     <li>GET    /api/users/{id}  - один пользователь</li>
 *     <li>POST   /api/users       - создать</li>
 *     <li>PATCH  /api/users/{id}  - частично обновить</li>
 *     <li>DELETE /api/users/{id}  - удалить</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/users")
@Tag(name = "Пользователи", description = "CRUD для пользователей")
public class UserController {

    private final UserService userService;
    private final UserModelAssembler assembler;

    public UserController(UserService userService, UserModelAssembler assembler) {
        this.userService = userService;
        this.assembler = assembler;
    }

    @GetMapping
    @Operation(summary = "Список всех пользователей")
    @ApiResponse(responseCode = "200", description = "Список пользователей")
    public CollectionModel<EntityModel<UserResponse>> getAll() {
        return assembler.toCollectionModel(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить пользователя по id")
    @ApiResponse(responseCode = "200", description = "Пользователь найден")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntityModel<UserResponse> getById(@Parameter(description = "Id пользователя", example = "1")
                                             @PathVariable Long id) {
        return assembler.toModel(userService.getUserById(id));
    }

    @PostMapping
    @Operation(summary = "Создать пользователя")
    @ApiResponse(responseCode = "201", description = "Пользователь создан, адрес в заголовке Location")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email уже занят",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<EntityModel<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        UserResponse created = userService.createUser(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(assembler.toModel(created));
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Частично обновить пользователя")
    @ApiResponse(responseCode = "200", description = "Пользователь обновлён")
    @ApiResponse(responseCode = "400", description = "Ошибка валидации",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Email уже занят",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public EntityModel<UserResponse> update(@Parameter(description = "Id пользователя", example = "1")
                                            @PathVariable Long id,
                                            @Valid @RequestBody UserUpdateRequest request) {
        return assembler.toModel(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Удалить пользователя")
    @ApiResponse(responseCode = "204", description = "Пользователь удалён")
    @ApiResponse(responseCode = "404", description = "Пользователь не найден",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> delete(@Parameter(description = "Id пользователя", example = "1")
                                       @PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

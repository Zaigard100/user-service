package zh.zaig.userservice.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import zh.zaig.userservice.user.dto.UserCreateRequest;
import zh.zaig.userservice.user.dto.UserResponse;
import zh.zaig.userservice.user.dto.UserUpdateRequest;
import zh.zaig.userservice.user.exception.EmailAlreadyExistsException;
import zh.zaig.userservice.user.exception.UserNotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@DisplayName("UserController")
class UserControllerTest {

    private static final String URL = "/api/users";
    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 10, 1, 12, 0);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    private static UserResponse sator() {
        return new UserResponse(1L, "Сатор Арепыч", "sator@mail.com", 54, CREATED_AT);
    }

    private static UserResponse rizvanov() {
        return new UserResponse(2L, "Ризванов Диаметр", "rizvanov@mail.com", 19, CREATED_AT);
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    @Nested
    @DisplayName("GET /api/users")
    class GetAll {

        @Test
        @DisplayName("возвращает 200 и список пользователей")
        void getAll_usersExist_returnsOkWithList() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of(sator(), rizvanov()));

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(2)))
                    .andExpect(jsonPath("$[0].name").value("Сатор Арепыч"))
                    .andExpect(jsonPath("$[1].email").value("rizvanov@mail.com"));
        }

        @Test
        @DisplayName("возвращает 200 и пустой список, если пользователей нет")
        void getAll_noUsers_returnsOkWithEmptyList() throws Exception {
            when(userService.getAllUsers()).thenReturn(List.of());

            mockMvc.perform(get(URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(0)));
        }
    }

    @Nested
    @DisplayName("GET /api/users/{id}")
    class GetById {

        @Test
        @DisplayName("возвращает 200 и пользователя")
        void getById_existingId_returnsOkWithUser() throws Exception {
            when(userService.getUserById(1L)).thenReturn(sator());

            mockMvc.perform(get(URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Сатор Арепыч"))
                    .andExpect(jsonPath("$.email").value("sator@mail.com"))
                    .andExpect(jsonPath("$.age").value(54))
                    .andExpect(jsonPath("$.createdAt").exists());
        }

        @Test
        @DisplayName("возвращает 404, если пользователя нет")
        void getById_missingId_returnsNotFound() throws Exception {
            when(userService.getUserById(42L)).thenThrow(new UserNotFoundException(42L));

            mockMvc.perform(get(URL + "/42"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value("Пользователь с id=42 не найден"));
        }

        @Test
        @DisplayName("возвращает 400, если id не число")
        void getById_nonNumericId_returnsBadRequest() throws Exception {
            mockMvc.perform(get(URL + "/abc"))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("POST /api/users")
    class Create {

        @Test
        @DisplayName("возвращает 201, созданного пользователя и Location")
        void create_validRequest_returnsCreatedWithLocation() throws Exception {
            UserCreateRequest request = new UserCreateRequest("Сатор Арепыч", "sator@mail.com", 54);
            when(userService.createUser(request)).thenReturn(sator());

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", endsWith("/api/users/1")))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Сатор Арепыч"));
        }

        @Test
        @DisplayName("возвращает 400 с ошибкой по полю, если имя пустое")
        void create_blankName_returnsBadRequestWithFieldError() throws Exception {
            UserCreateRequest request = new UserCreateRequest("  ", "sator@mail.com", 54);

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.name").value("Имя не может быть пустым"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("возвращает 400, если email некорректный")
        void create_invalidEmail_returnsBadRequest() throws Exception {
            UserCreateRequest request = new UserCreateRequest("Сатор Арепыч", "sator@mail", 54);

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.email").value("Некорректный формат email"));

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("возвращает 400, если возраст вне диапазона")
        void create_ageOutOfRange_returnsBadRequest() throws Exception {
            UserCreateRequest request = new UserCreateRequest("Сатор Арепыч", "sator@mail.com", 151);

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.age").exists());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("возвращает 409, если email уже занят")
        void create_emailAlreadyExists_returnsConflict() throws Exception {
            UserCreateRequest request = new UserCreateRequest("Сатор Арепыч", "sator@mail.com", 54);
            when(userService.createUser(request)).thenThrow(new EmailAlreadyExistsException("sator@mail.com"));

            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.status").value(409));
        }

        @Test
        @DisplayName("возвращает 400, если тело запроса не JSON")
        void create_malformedJson_returnsBadRequest() throws Exception {
            mockMvc.perform(post(URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{name: oops"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Некорректное тело запроса"));

            verifyNoInteractions(userService);
        }
    }

    @Nested
    @DisplayName("PATCH /api/users/{id}")
    class Update {

        @Test
        @DisplayName("возвращает 200 и обновлённого пользователя")
        void update_validRequest_returnsOkWithUpdatedUser() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("Шварев Нейрослоп", null, 22);
            UserResponse updated = new UserResponse(1L, "Шварев Нейрослоп", "sator@mail.com", 22, CREATED_AT);
            when(userService.updateUser(1L, request)).thenReturn(updated);

            mockMvc.perform(patch(URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Шварев Нейрослоп"))
                    .andExpect(jsonPath("$.age").value(22));
        }

        @Test
        @DisplayName("возвращает 404, если пользователя нет")
        void update_missingId_returnsNotFound() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest("Шварев Нейрослоп", null, null);
            when(userService.updateUser(eq(42L), any())).thenThrow(new UserNotFoundException(42L));

            mockMvc.perform(patch(URL + "/42")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("возвращает 400, если возраст некорректный")
        void update_invalidAge_returnsBadRequest() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest(null, null, -1);

            mockMvc.perform(patch(URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors.age").exists());

            verifyNoInteractions(userService);
        }

        @Test
        @DisplayName("возвращает 409, если email занят другим пользователем")
        void update_emailTakenByAnotherUser_returnsConflict() throws Exception {
            UserUpdateRequest request = new UserUpdateRequest(null, "rizvanov@mail.com", null);
            when(userService.updateUser(1L, request)).thenThrow(new EmailAlreadyExistsException("rizvanov@mail.com"));

            mockMvc.perform(patch(URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(request)))
                    .andExpect(status().isConflict());
        }
    }

    @Nested
    @DisplayName("DELETE /api/users/{id}")
    class Delete {

        @Test
        @DisplayName("возвращает 204, если пользователь удалён")
        void delete_existingId_returnsNoContent() throws Exception {
            mockMvc.perform(delete(URL + "/1"))
                    .andExpect(status().isNoContent());

            verify(userService).deleteUser(1L);
        }

        @Test
        @DisplayName("возвращает 404, если пользователя нет")
        void delete_missingId_returnsNotFound() throws Exception {
            doThrow(new UserNotFoundException(42L)).when(userService).deleteUser(42L);

            mockMvc.perform(delete(URL + "/42"))
                    .andExpect(status().isNotFound());
        }
    }
}

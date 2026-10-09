package zh.zaig.userservice.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import zh.zaig.userservice.TestcontainersConfiguration;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@DisplayName("User API (интеграционные тесты с БД)")
class UserApiIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST сохраняет пользователя в БД")
    void create_validRequest_persistsUser() throws Exception {
        String body = """
                {"name": "Сатор Арепыч", "email": "sator@mail.com", "age": 54}
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber());

        User saved = userRepository.findByEmail("sator@mail.com").orElseThrow();
        assertAll(
                () -> assertEquals("Сатор Арепыч", saved.getName()),
                () -> assertEquals(54, saved.getAge())
        );
    }

    @Test
    @DisplayName("POST возвращает 409, если email уже есть в БД")
    void create_duplicateEmail_returnsConflict() throws Exception {
        userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));
        String body = """
                {"name": "Ризванов Диаметр", "email": "sator@mail.com", "age": 19}
                """;

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());

        assertEquals(1, userRepository.count());
    }

    @Test
    @DisplayName("GET возвращает всех пользователей из БД")
    void getAll_usersInDatabase_returnsAllUsers() throws Exception {
        userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));
        userRepository.save(new User("Ризванов Диаметр", "rizvanov@mail.com", 19));
        userRepository.save(new User("Шварев Нейрослоп", "shvarev@mail.com", 22));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].name").value("Шварев Нейрослоп"));
    }

    @Test
    @DisplayName("PATCH меняет только переданные поля в БД")
    void update_partialRequest_changesOnlyGivenFields() throws Exception {
        User sator = userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));
        String body = """
                {"age": 55}
                """;

        mockMvc.perform(patch("/api/users/" + sator.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        User updated = userRepository.findById(sator.getId()).orElseThrow();
        assertAll(
                () -> assertEquals("Сатор Арепыч", updated.getName()),
                () -> assertEquals("sator@mail.com", updated.getEmail()),
                () -> assertEquals(55, updated.getAge())
        );
    }

    @Test
    @DisplayName("DELETE удаляет пользователя из БД")
    void delete_existingUser_removesUser() throws Exception {
        User sator = userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));

        mockMvc.perform(delete("/api/users/" + sator.getId()))
                .andExpect(status().isNoContent());

        assertTrue(userRepository.findById(sator.getId()).isEmpty());
    }

    @Test
    @DisplayName("GET по id возвращает 404 для несуществующего пользователя")
    void getById_missingUser_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/users/999"))
                .andExpect(status().isNotFound());
    }
}

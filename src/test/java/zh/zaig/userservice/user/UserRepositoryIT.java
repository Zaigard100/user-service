package zh.zaig.userservice.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import zh.zaig.userservice.TestcontainersConfiguration;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
@DisplayName("UserRepository (интеграционные тесты)")
class UserRepositoryIT {

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("save присваивает id и дату создания")
    void save_newUser_assignsIdAndCreatedAt() {
        User user = new User("Сатор Арепыч", "sator@mail.com", 54);

        User saved = userRepository.saveAndFlush(user);

        assertAll(
                () -> assertNotNull(saved.getId()),
                () -> assertNotNull(saved.getCreatedAt())
        );
    }

    @Test
    @DisplayName("save бросает исключение при дублировании email")
    void save_duplicateEmail_throwsDataIntegrityViolationException() {
        userRepository.saveAndFlush(new User("Сатор Арепыч", "sator@mail.com", 54));
        User duplicate = new User("Ризванов Диаметр", "sator@mail.com", 19);

        assertThrows(DataIntegrityViolationException.class, () -> userRepository.saveAndFlush(duplicate));
    }

    @Test
    @DisplayName("findByEmail находит пользователя по email")
    void findByEmail_existingEmail_returnsUser() {
        User saved = userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));

        Optional<User> result = userRepository.findByEmail("sator@mail.com");

        assertTrue(result.isPresent());
        assertEquals(saved.getId(), result.get().getId());
    }

    @Test
    @DisplayName("findByEmail возвращает пустой Optional для неизвестного email")
    void findByEmail_missingEmail_returnsEmpty() {
        userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));

        Optional<User> result = userRepository.findByEmail("rizvanov@mail.com");

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("existsByEmail возвращает true, если email занят")
    void existsByEmail_existingEmail_returnsTrue() {
        userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));

        boolean exists = userRepository.existsByEmail("sator@mail.com");

        assertTrue(exists);
    }

    @Test
    @DisplayName("existsByEmailAndIdNot возвращает false для своего же email")
    void existsByEmailAndIdNot_emailOfSameUser_returnsFalse() {
        User sator = userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));

        boolean exists = userRepository.existsByEmailAndIdNot("sator@mail.com", sator.getId());

        assertFalse(exists);
    }

    @Test
    @DisplayName("existsByEmailAndIdNot возвращает true, если email у другого пользователя")
    void existsByEmailAndIdNot_emailOfAnotherUser_returnsTrue() {
        userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));
        User rizvanov = userRepository.save(new User("Ризванов Диаметр", "rizvanov@mail.com", 19));

        boolean exists = userRepository.existsByEmailAndIdNot("sator@mail.com", rizvanov.getId());

        assertTrue(exists);
    }

    @Test
    @DisplayName("findAllByOrderByIdAsc возвращает пользователей по возрастанию id")
    void findAllByOrderByIdAsc_severalUsers_returnsUsersOrderedById() {
        User first = userRepository.save(new User("Сатор Арепыч", "sator@mail.com", 54));
        User second = userRepository.save(new User("Ризванов Диаметр", "rizvanov@mail.com", 19));
        User third = userRepository.save(new User("Шварев Нейрослоп", "shvarev@mail.com", 22));

        List<User> result = userRepository.findAllByOrderByIdAsc();

        assertEquals(List.of(first.getId(), second.getId(), third.getId()),
                result.stream().map(User::getId).toList());
    }
}

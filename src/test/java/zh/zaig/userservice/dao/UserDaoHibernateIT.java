package zh.zaig.userservice.dao;

import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import zh.zaig.userservice.entity.User;
import zh.zaig.userservice.exception.DataAccessException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@DisplayName("UserDaoHibernate (интеграционные тесты)")
class UserDaoHibernateIT {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    private static SessionFactory sessionFactory;

    private UserDao userDao;

    @BeforeAll
    static void createSessionFactory() {
        sessionFactory = new Configuration()
                .addAnnotatedClass(User.class)
                .setProperty("hibernate.connection.url", POSTGRES.getJdbcUrl())
                .setProperty("hibernate.connection.username", POSTGRES.getUsername())
                .setProperty("hibernate.connection.password", POSTGRES.getPassword())
                .setProperty("hibernate.hbm2ddl.auto", "create-drop")
                .buildSessionFactory();
    }

    @AfterAll
    static void closeSessionFactory() {
        sessionFactory.close();
    }

    @BeforeEach
    void cleanDatabase() {
        sessionFactory.inTransaction(session ->
                session.createNativeMutationQuery("TRUNCATE TABLE users RESTART IDENTITY").executeUpdate());
        userDao = new UserDaoHibernate(sessionFactory);
    }

    @Nested
    @DisplayName("save")
    class Save {

        @Test
        @DisplayName("сохраняет пользователя и присваивает id и дату создания")
        void save_newUser_assignsIdAndCreatedAt() {
            User user = new User("Сатор Арепыч", "sator@mail.com", 54);

            User saved = userDao.save(user);

            assertAll(
                    () -> assertNotNull(saved.getId()),
                    () -> assertNotNull(saved.getCreatedAt()),
                    () -> assertTrue(userDao.findById(saved.getId()).isPresent())
            );
        }

        @Test
        @DisplayName("бросает DataAccessException при дублировании email")
        void save_duplicateEmail_throwsDataAccessException() {
            userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));
            User duplicate = new User("Ризванов Диаметр", "sator@mail.com", 19);

            assertThrows(DataAccessException.class, () -> userDao.save(duplicate));

            assertEquals(1, userDao.findAll().size());
        }

        @Test
        @DisplayName("бросает DataAccessException, если имя не указано")
        void save_nullName_throwsDataAccessException() {
            User user = new User(null, "sator@mail.com", 54);

            assertThrows(DataAccessException.class, () -> userDao.save(user));

            assertTrue(userDao.findAll().isEmpty());
        }
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("возвращает пользователя по существующему id")
        void findById_existingId_returnsUser() {
            User saved = userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));

            Optional<User> result = userDao.findById(saved.getId());

            assertTrue(result.isPresent());
            assertAll(
                    () -> assertEquals("Сатор Арепыч", result.get().getName()),
                    () -> assertEquals("sator@mail.com", result.get().getEmail()),
                    () -> assertEquals(54, result.get().getAge())
            );
        }

        @Test
        @DisplayName("возвращает пустой Optional, если пользователя нет")
        void findById_missingId_returnsEmpty() {
            Optional<User> result = userDao.findById(999L);

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findByEmail")
    class FindByEmail {

        @Test
        @DisplayName("возвращает пользователя по существующему email")
        void findByEmail_existingEmail_returnsUser() {
            User saved = userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));

            Optional<User> result = userDao.findByEmail("sator@mail.com");

            assertTrue(result.isPresent());
            assertEquals(saved.getId(), result.get().getId());
        }

        @Test
        @DisplayName("возвращает пустой Optional, если email не найден")
        void findByEmail_missingEmail_returnsEmpty() {
            userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));

            Optional<User> result = userDao.findByEmail("rizvanov@mail.com");

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("возвращает всех пользователей, отсортированных по id")
        void findAll_severalUsers_returnsUsersOrderedById() {
            User first = userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));
            User second = userDao.save(new User("Ризванов Диаметр", "rizvanov@mail.com", 19));
            User third = userDao.save(new User("Шварев Нейрослоп", "shvarev@mail.com", 22));

            List<User> result = userDao.findAll();

            assertEquals(List.of(first.getId(), second.getId(), third.getId()),
                    result.stream().map(User::getId).toList());
        }

        @Test
        @DisplayName("возвращает пустой список, если таблица пуста")
        void findAll_noUsers_returnsEmptyList() {
            List<User> result = userDao.findAll();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("сохраняет изменённые поля в БД")
        void update_existingUser_persistsChanges() {
            User saved = userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));
            saved.setName("Шварев Нейрослоп");
            saved.setEmail("shvarev@mail.com");
            saved.setAge(22);

            userDao.update(saved);

            User reloaded = userDao.findById(saved.getId()).orElseThrow();
            assertAll(
                    () -> assertEquals("Шварев Нейрослоп", reloaded.getName()),
                    () -> assertEquals("shvarev@mail.com", reloaded.getEmail()),
                    () -> assertEquals(22, reloaded.getAge())
            );
        }

        @Test
        @DisplayName("бросает DataAccessException, если email занят другим пользователем")
        void update_emailTakenByAnotherUser_throwsDataAccessException() {
            userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));
            User rizvanov = userDao.save(new User("Ризванов Диаметр", "rizvanov@mail.com", 19));
            rizvanov.setEmail("sator@mail.com");

            assertThrows(DataAccessException.class, () -> userDao.update(rizvanov));

            assertEquals("rizvanov@mail.com", userDao.findById(rizvanov.getId()).orElseThrow().getEmail());
        }
    }

    @Nested
    @DisplayName("deleteById")
    class DeleteById {

        @Test
        @DisplayName("удаляет существующего пользователя и возвращает true")
        void deleteById_existingId_deletesUserAndReturnsTrue() {
            User saved = userDao.save(new User("Сатор Арепыч", "sator@mail.com", 54));

            boolean deleted = userDao.deleteById(saved.getId());

            assertTrue(deleted);
            assertTrue(userDao.findById(saved.getId()).isEmpty());
        }

        @Test
        @DisplayName("возвращает false, если пользователя нет")
        void deleteById_missingId_returnsFalse() {
            boolean deleted = userDao.deleteById(999L);

            assertFalse(deleted);
        }
    }
}

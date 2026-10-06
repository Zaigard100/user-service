package zh.zaig.userservice.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import zh.zaig.userservice.dao.UserDao;
import zh.zaig.userservice.entity.User;
import zh.zaig.userservice.exception.UserNotFoundException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

    private static User user(Long id, String name, String email, Integer age) {
        User user = new User(name, email, age);
        user.setId(id);
        return user;
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("сохраняет пользователя с корректными данными")
        void createUser_validData_returnsSavedUser() {
            User saved = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userDao.findByEmail("sator@mail.com")).thenReturn(Optional.empty());
            when(userDao.save(any(User.class))).thenReturn(saved);

            User result = userService.createUser("Сатор Арепыч", "sator@mail.com", 54);

            assertSame(saved, result);
        }

        @Test
        @DisplayName("обрезает пробелы в имени и email перед сохранением")
        void createUser_valuesWithSpaces_savesTrimmedValues() {
            when(userDao.findByEmail(anyString())).thenReturn(Optional.empty());
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            userService.createUser("  Сатор Арепыч  ", " sator@mail.com ", 54);

            verify(userDao).save(captor.capture());
            User passed = captor.getValue();
            assertAll(
                    () -> assertEquals("Сатор Арепыч", passed.getName()),
                    () -> assertEquals("sator@mail.com", passed.getEmail()),
                    () -> assertEquals(54, passed.getAge())
            );
        }

        @Test
        @DisplayName("разрешает не указывать возраст")
        void createUser_nullAge_savesUserWithoutAge() {
            when(userDao.findByEmail(anyString())).thenReturn(Optional.empty());
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            userService.createUser("Сатор Арепыч", "sator@mail.com", null);

            verify(userDao).save(captor.capture());
            assertNull(captor.getValue().getAge());
        }

        @Test
        @DisplayName("бросает исключение, если email уже занят")
        void createUser_emailAlreadyExists_throwsIllegalArgumentException() {
            when(userDao.findByEmail("sator@mail.com"))
                    .thenReturn(Optional.of(user(5L, "Шварев Нейрослоп", "sator@mail.com", 22)));

            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser("Сатор Арепыч", "sator@mail.com", 54));

            assertTrue(exception.getMessage().contains("уже существует"));
            verify(userDao, never()).save(any());
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("бросает исключение при пустом имени")
        void createUser_blankName_throwsIllegalArgumentException(String name) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser(name, "sator@mail.com", 54));

            verifyNoInteractions(userDao);
        }

        @Test
        @DisplayName("бросает исключение, если имя длиннее 100 символов")
        void createUser_tooLongName_throwsIllegalArgumentException() {
            String longName = "a".repeat(101);

            assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser(longName, "sator@mail.com", 54));

            verifyNoInteractions(userDao);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "sator", "sator@", "@mail.com", "sator@mail", "sator@mail.c", "sa tor@mail.com"})
        @DisplayName("бросает исключение при некорректном email")
        void createUser_invalidEmail_throwsIllegalArgumentException(String email) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser("Сатор Арепыч", email, 54));

            verifyNoInteractions(userDao);
        }

        @ParameterizedTest
        @ValueSource(ints = {-1, 151})
        @DisplayName("бросает исключение при возрасте вне диапазона 0..150")
        void createUser_ageOutOfRange_throwsIllegalArgumentException(int age) {
            assertThrows(IllegalArgumentException.class,
                    () -> userService.createUser("Сатор Арепыч", "sator@mail.com", age));

            verifyNoInteractions(userDao);
        }

        @ParameterizedTest
        @ValueSource(ints = {0, 150})
        @DisplayName("принимает граничные значения возраста")
        void createUser_boundaryAge_savesUser(int age) {
            when(userDao.findByEmail(anyString())).thenReturn(Optional.empty());

            userService.createUser("Сатор Арепыч", "sator@mail.com", age);

            verify(userDao).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("возвращает пользователя, если он существует")
        void getUserById_existingId_returnsUser() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userDao.findById(1L)).thenReturn(Optional.of(existing));

            User result = userService.getUserById(1L);

            assertSame(existing, result);
        }

        @Test
        @DisplayName("бросает UserNotFoundException, если пользователя нет")
        void getUserById_missingId_throwsUserNotFoundException() {
            when(userDao.findById(42L)).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                    () -> userService.getUserById(42L));

            assertTrue(exception.getMessage().contains("42"));
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("возвращает список из DAO")
        void getAllUsers_usersExist_returnsAllUsers() {
            List<User> users = List.of(
                    user(1L, "Сатор Арепыч", "sator@mail.com", 54),
                    user(2L, "Ризванов Диаметр", "rizvanov@mail.com", 19)
            );
            when(userDao.findAll()).thenReturn(users);

            List<User> result = userService.getAllUsers();

            assertEquals(users, result);
        }

        @Test
        @DisplayName("возвращает пустой список, если пользователей нет")
        void getAllUsers_noUsers_returnsEmptyList() {
            when(userDao.findAll()).thenReturn(List.of());

            List<User> result = userService.getAllUsers();

            assertTrue(result.isEmpty());
        }
    }

    @Nested
    @DisplayName("updateUser")
    class UpdateUser {

        @Test
        @DisplayName("обновляет все переданные поля")
        void updateUser_allFieldsProvided_updatesAllFields() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userDao.findById(1L)).thenReturn(Optional.of(existing));
            when(userDao.findByEmail("new@mail.com")).thenReturn(Optional.empty());
            when(userDao.update(existing)).thenReturn(existing);

            User result = userService.updateUser(1L, " Ризванов Диаметр ", "new@mail.com", 19);

            assertAll(
                    () -> assertEquals("Ризванов Диаметр", result.getName()),
                    () -> assertEquals("new@mail.com", result.getEmail()),
                    () -> assertEquals(19, result.getAge())
            );
        }

        @Test
        @DisplayName("не меняет поля, переданные как null или пустая строка")
        void updateUser_emptyFields_keepsOldValues() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userDao.findById(1L)).thenReturn(Optional.of(existing));
            when(userDao.update(existing)).thenReturn(existing);

            User result = userService.updateUser(1L, "", null, null);

            assertAll(
                    () -> assertEquals("Сатор Арепыч", result.getName()),
                    () -> assertEquals("sator@mail.com", result.getEmail()),
                    () -> assertEquals(54, result.getAge())
            );
            verify(userDao, never()).findByEmail(any());
        }

        @Test
        @DisplayName("разрешает оставить свой же email")
        void updateUser_sameEmailOfSameUser_updatesUser() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userDao.findById(1L)).thenReturn(Optional.of(existing));
            when(userDao.findByEmail("sator@mail.com")).thenReturn(Optional.of(existing));
            when(userDao.update(existing)).thenReturn(existing);

            User result = userService.updateUser(1L, null, "sator@mail.com", null);

            assertEquals("sator@mail.com", result.getEmail());
        }

        @Test
        @DisplayName("бросает исключение, если email занят другим пользователем")
        void updateUser_emailTakenByAnotherUser_throwsIllegalArgumentException() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            User another = user(2L, "Ризванов Диаметр", "rizvanov@mail.com", 19);
            when(userDao.findById(1L)).thenReturn(Optional.of(existing));
            when(userDao.findByEmail("rizvanov@mail.com")).thenReturn(Optional.of(another));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, null, "rizvanov@mail.com", null));

            verify(userDao, never()).update(any());
        }

        @Test
        @DisplayName("бросает UserNotFoundException, если пользователя нет")
        void updateUser_missingId_throwsUserNotFoundException() {
            when(userDao.findById(42L)).thenReturn(Optional.empty());

            assertThrows(UserNotFoundException.class,
                    () -> userService.updateUser(42L, "Ризванов Диаметр", null, null));

            verify(userDao, never()).update(any());
        }

        @Test
        @DisplayName("бросает исключение при некорректном email")
        void updateUser_invalidEmail_throwsIllegalArgumentException() {
            when(userDao.findById(1L)).thenReturn(Optional.of(user(1L, "Сатор Арепыч", "sator@mail.com", 54)));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, null, "not-an-email", null));

            verify(userDao, never()).update(any());
        }

        @Test
        @DisplayName("бросает исключение при некорректном возрасте")
        void updateUser_invalidAge_throwsIllegalArgumentException() {
            when(userDao.findById(1L)).thenReturn(Optional.of(user(1L, "Сатор Арепыч", "sator@mail.com", 54)));

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, null, null, 200));

            verify(userDao, never()).update(any());
        }

        @Test
        @DisplayName("бросает исключение, если новое имя длиннее 100 символов")
        void updateUser_tooLongName_throwsIllegalArgumentException() {
            when(userDao.findById(1L)).thenReturn(Optional.of(user(1L, "Сатор Арепыч", "sator@mail.com", 54)));
            String longName = "a".repeat(101);

            assertThrows(IllegalArgumentException.class,
                    () -> userService.updateUser(1L, longName, null, null));

            verify(userDao, never()).update(any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("удаляет существующего пользователя")
        void deleteUser_existingId_deletesUser() {
            when(userDao.deleteById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userDao).deleteById(1L);
        }

        @Test
        @DisplayName("бросает UserNotFoundException, если пользователя нет")
        void deleteUser_missingId_throwsUserNotFoundException() {
            when(userDao.deleteById(42L)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> userService.deleteUser(42L));
        }
    }
}

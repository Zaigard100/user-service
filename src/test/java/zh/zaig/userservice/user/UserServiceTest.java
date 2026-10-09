package zh.zaig.userservice.user;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import zh.zaig.userservice.user.dto.UserCreateRequest;
import zh.zaig.userservice.user.dto.UserResponse;
import zh.zaig.userservice.user.dto.UserUpdateRequest;
import zh.zaig.userservice.user.exception.EmailAlreadyExistsException;
import zh.zaig.userservice.user.exception.UserNotFoundException;
import zh.zaig.userservice.user.mapper.UserMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Spy
    private UserMapper userMapper = new UserMapper();

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
        @DisplayName("сохраняет пользователя и возвращает DTO с id")
        void createUser_validRequest_returnsResponseWithId() {
            UserCreateRequest request = new UserCreateRequest("Сатор Арепыч", "sator@mail.com", 54);
            when(userRepository.existsByEmail("sator@mail.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(user(1L, "Сатор Арепыч", "sator@mail.com", 54));

            UserResponse result = userService.createUser(request);

            assertAll(
                    () -> assertEquals(1L, result.id()),
                    () -> assertEquals("Сатор Арепыч", result.name()),
                    () -> assertEquals("sator@mail.com", result.email()),
                    () -> assertEquals(54, result.age())
            );
        }

        @Test
        @DisplayName("обрезает пробелы в имени и email перед сохранением")
        void createUser_valuesWithSpaces_savesTrimmedValues() {
            UserCreateRequest request = new UserCreateRequest("  Сатор Арепыч  ", " sator@mail.com ", 54);
            when(userRepository.existsByEmail("sator@mail.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);

            userService.createUser(request);

            verify(userRepository).save(captor.capture());
            assertAll(
                    () -> assertEquals("Сатор Арепыч", captor.getValue().getName()),
                    () -> assertEquals("sator@mail.com", captor.getValue().getEmail())
            );
        }

        @Test
        @DisplayName("бросает EmailAlreadyExistsException, если email занят")
        void createUser_emailAlreadyExists_throwsEmailAlreadyExistsException() {
            UserCreateRequest request = new UserCreateRequest("Сатор Арепыч", "sator@mail.com", 54);
            when(userRepository.existsByEmail("sator@mail.com")).thenReturn(true);

            assertThrows(EmailAlreadyExistsException.class, () -> userService.createUser(request));

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("возвращает DTO пользователя по id")
        void getUserById_existingId_returnsResponse() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Сатор Арепыч", "sator@mail.com", 54)));

            UserResponse result = userService.getUserById(1L);

            assertEquals("Сатор Арепыч", result.name());
        }

        @Test
        @DisplayName("бросает UserNotFoundException, если пользователя нет")
        void getUserById_missingId_throwsUserNotFoundException() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());

            UserNotFoundException exception = assertThrows(UserNotFoundException.class,
                    () -> userService.getUserById(42L));

            assertTrue(exception.getMessage().contains("42"));
        }
    }

    @Nested
    @DisplayName("getAllUsers")
    class GetAllUsers {

        @Test
        @DisplayName("возвращает всех пользователей в виде DTO")
        void getAllUsers_usersExist_returnsAllResponses() {
            when(userRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                    user(1L, "Сатор Арепыч", "sator@mail.com", 54),
                    user(2L, "Ризванов Диаметр", "rizvanov@mail.com", 19)
            ));

            List<UserResponse> result = userService.getAllUsers();

            assertEquals(List.of(1L, 2L), result.stream().map(UserResponse::id).toList());
        }

        @Test
        @DisplayName("возвращает пустой список, если пользователей нет")
        void getAllUsers_noUsers_returnsEmptyList() {
            when(userRepository.findAllByOrderByIdAsc()).thenReturn(List.of());

            List<UserResponse> result = userService.getAllUsers();

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
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.existsByEmailAndIdNot("rizvanov@mail.com", 1L)).thenReturn(false);
            when(userRepository.save(existing)).thenReturn(existing);
            UserUpdateRequest request = new UserUpdateRequest(" Ризванов Диаметр ", "rizvanov@mail.com", 19);

            UserResponse result = userService.updateUser(1L, request);

            assertAll(
                    () -> assertEquals("Ризванов Диаметр", result.name()),
                    () -> assertEquals("rizvanov@mail.com", result.email()),
                    () -> assertEquals(19, result.age())
            );
        }

        @Test
        @DisplayName("не меняет поля, переданные как null или пустая строка")
        void updateUser_emptyFields_keepsOldValues() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.save(existing)).thenReturn(existing);
            UserUpdateRequest request = new UserUpdateRequest("", null, null);

            UserResponse result = userService.updateUser(1L, request);

            assertAll(
                    () -> assertEquals("Сатор Арепыч", result.name()),
                    () -> assertEquals("sator@mail.com", result.email()),
                    () -> assertEquals(54, result.age())
            );
            verify(userRepository, never()).existsByEmailAndIdNot(anyString(), anyLong());
        }

        @Test
        @DisplayName("разрешает оставить свой же email")
        void updateUser_sameEmailOfSameUser_updatesUser() {
            User existing = user(1L, "Сатор Арепыч", "sator@mail.com", 54);
            when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
            when(userRepository.existsByEmailAndIdNot("sator@mail.com", 1L)).thenReturn(false);
            when(userRepository.save(existing)).thenReturn(existing);
            UserUpdateRequest request = new UserUpdateRequest(null, "sator@mail.com", null);

            UserResponse result = userService.updateUser(1L, request);

            assertEquals("sator@mail.com", result.email());
        }

        @Test
        @DisplayName("бросает EmailAlreadyExistsException, если email занят другим пользователем")
        void updateUser_emailTakenByAnotherUser_throwsEmailAlreadyExistsException() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "Сатор Арепыч", "sator@mail.com", 54)));
            when(userRepository.existsByEmailAndIdNot("rizvanov@mail.com", 1L)).thenReturn(true);
            UserUpdateRequest request = new UserUpdateRequest(null, "rizvanov@mail.com", null);

            assertThrows(EmailAlreadyExistsException.class, () -> userService.updateUser(1L, request));

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("бросает UserNotFoundException, если пользователя нет")
        void updateUser_missingId_throwsUserNotFoundException() {
            when(userRepository.findById(42L)).thenReturn(Optional.empty());
            UserUpdateRequest request = new UserUpdateRequest("Шварев Нейрослоп", null, null);

            assertThrows(UserNotFoundException.class, () -> userService.updateUser(42L, request));

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUser {

        @Test
        @DisplayName("удаляет существующего пользователя")
        void deleteUser_existingId_deletesUser() {
            when(userRepository.existsById(1L)).thenReturn(true);

            userService.deleteUser(1L);

            verify(userRepository).deleteById(1L);
        }

        @Test
        @DisplayName("бросает UserNotFoundException, если пользователя нет")
        void deleteUser_missingId_throwsUserNotFoundException() {
            when(userRepository.existsById(42L)).thenReturn(false);

            assertThrows(UserNotFoundException.class, () -> userService.deleteUser(42L));

            verify(userRepository, never()).deleteById(any());
        }
    }
}

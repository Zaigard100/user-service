package zh.zaig.userservice.service;

import zh.zaig.userservice.dao.UserDao;
import zh.zaig.userservice.dao.UserDaoHibernate;
import zh.zaig.userservice.entity.User;
import zh.zaig.userservice.exception.UserNotFoundException;

import java.util.List;
import java.util.regex.Pattern;

public class UserService {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");

    private final UserDao userDao;

    public UserService() {
        this(new UserDaoHibernate());
    }

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Создаёт пользователя после проверки полей и уникальности email.
     *
     * @throws IllegalArgumentException если данные некорректны или email уже занят
     */
    public User createUser(String name, String email, Integer age) {
        validateName(name);
        validateEmail(email);
        validateAge(age);

        userDao.findByEmail(email).ifPresent(u -> {
            throw new IllegalArgumentException("Пользователь с email '" + email + "' уже существует");
        });

        return userDao.save(new User(name.trim(), email.trim(), age));
    }

    public User getUserById(Long id) {
        return userDao.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    public List<User> getAllUsers() {
        return userDao.findAll();
    }

    /**
     * Обновляет только переданные поля: null или пустая строка означает "не менять".
     *
     * @throws UserNotFoundException    если пользователя с таким id нет
     * @throws IllegalArgumentException если новые данные некорректны или email занят другим пользователем
     */
    public User updateUser(Long id, String name, String email, Integer age) {
        User existing = getUserById(id);

        if (name != null && !name.isBlank()) {
            validateName(name);
            existing.setName(name.trim());
        }
        if (email != null && !email.isBlank()) {
            validateEmail(email);
            userDao.findByEmail(email)
                    .filter(u -> !u.getId().equals(id))
                    .ifPresent(u -> {
                        throw new IllegalArgumentException("Email '" + email + "' уже используется другим пользователем");
                    });
            existing.setEmail(email.trim());
        }
        if (age != null) {
            validateAge(age);
            existing.setAge(age);
        }

        return userDao.update(existing);
    }

    public void deleteUser(Long id) {
        if (!userDao.deleteById(id)) {
            throw new UserNotFoundException(id);
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Имя не может быть пустым");
        }
        if (name.trim().length() > 100) {
            throw new IllegalArgumentException("Имя не может быть длиннее 100 символов");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email не может быть пустым");
        }
        if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            throw new IllegalArgumentException("Некорректный формат email: " + email);
        }
    }

    private void validateAge(Integer age) {
        if (age != null && (age < 0 || age > 150)) {
            throw new IllegalArgumentException("Возраст должен быть от 0 до 150");
        }
    }
}

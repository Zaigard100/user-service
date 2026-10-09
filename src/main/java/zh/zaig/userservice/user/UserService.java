package zh.zaig.userservice.user;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import zh.zaig.userservice.user.dto.UserCreateRequest;
import zh.zaig.userservice.user.dto.UserResponse;
import zh.zaig.userservice.user.dto.UserUpdateRequest;
import zh.zaig.userservice.user.exception.EmailAlreadyExistsException;
import zh.zaig.userservice.user.exception.UserNotFoundException;
import zh.zaig.userservice.user.mapper.UserMapper;

import java.util.List;

/**
 * Бизнес-логика по пользователям.
 */
@Service
@Transactional(readOnly = true)
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public UserService(UserRepository userRepository, UserMapper userMapper) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
    }

    /**
     * @throws EmailAlreadyExistsException если email уже занят
     */
    @Transactional
    public UserResponse createUser(UserCreateRequest request) {
        String email = request.email().trim();
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }

        User saved = userRepository.save(userMapper.toEntity(request));
        log.info("Создан пользователь: {}", saved);
        return userMapper.toResponse(saved);
    }

    public UserResponse getUserById(Long id) {
        return userMapper.toResponse(findUser(id));
    }

    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByIdAsc().stream()
                .map(userMapper::toResponse)
                .toList();
    }

    /**
     * Обновляет только переданные поля, null или пустая строка значит "не менять".
     *
     * @throws UserNotFoundException       если пользователя нет
     * @throws EmailAlreadyExistsException если email занят другим пользователем
     */
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        User user = findUser(id);

        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String email = request.email().trim();
            if (userRepository.existsByEmailAndIdNot(email, id)) {
                throw new EmailAlreadyExistsException(email);
            }
            user.setEmail(email);
        }
        if (request.age() != null) {
            user.setAge(request.age());
        }

        User updated = userRepository.save(user);
        log.info("Обновлён пользователь: {}", updated);
        return userMapper.toResponse(updated);
    }

    /**
     * @throws UserNotFoundException если пользователя нет
     */
    @Transactional
    public void deleteUser(Long id) {
        if (!userRepository.existsById(id)) {
            throw new UserNotFoundException(id);
        }
        userRepository.deleteById(id);
        log.info("Удалён пользователь с id={}", id);
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }
}

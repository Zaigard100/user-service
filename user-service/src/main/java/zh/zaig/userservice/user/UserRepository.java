package zh.zaig.userservice.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /**
     * Есть ли такой email у кого-то кроме пользователя с данным id.
     * Нужно при обновлении, чтобы можно было "сохранить" свой же email.
     */
    boolean existsByEmailAndIdNot(String email, Long id);

    List<User> findAllByOrderByIdAsc();
}

package zh.zaig.userservice.dao;

import zh.zaig.userservice.entity.User;

import java.util.Optional;

public interface UserDao extends GenericDao<User, Long> {

    Optional<User> findByEmail(String email);
}

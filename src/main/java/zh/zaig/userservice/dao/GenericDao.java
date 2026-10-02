package zh.zaig.userservice.dao;

import java.util.List;
import java.util.Optional;

public interface GenericDao<T, ID> {

    T save(T entity);

    Optional<T> findById(ID id);

    List<T> findAll();

    T update(T entity);

    /**
     * @return true, если сущность была найдена и удалена
     */
    boolean deleteById(ID id);
}

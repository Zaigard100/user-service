package zh.zaig.userservice.dao;

import zh.zaig.userservice.entity.User;
import zh.zaig.userservice.exception.DataAccessException;
import zh.zaig.userservice.util.HibernateUtil;
import jakarta.persistence.PersistenceException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Реализация {@link UserDao} на Hibernate. Каждый метод работает в своей
 * сессии и транзакции, ошибки Hibernate/JDBC оборачиваются в {@link DataAccessException}.
 */
public class UserDaoHibernate implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoHibernate.class);

    private final SessionFactory sessionFactory;

    public UserDaoHibernate() {
        this(HibernateUtil.getSessionFactory());
    }

    public UserDaoHibernate(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }

    @Override
    public User save(User user) {
        return executeInTransaction(session -> {
            session.persist(user);
            log.info("Создан пользователь: {}", user);
            return user;
        }, "создании пользователя");
    }

    @Override
    public Optional<User> findById(Long id) {
        return executeInTransaction(session -> Optional.ofNullable(session.get(User.class, id)),
                "поиске пользователя по id");
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return executeInTransaction(
            session -> {
                Query<User> query = session.createQuery("FROM User u WHERE u.email = :email", User.class);
                query.setParameter("email", email);
                return query.uniqueResultOptional();
            },
            "поиске пользователя по email"
        );
    }

    @Override
    public List<User> findAll() {
        return executeInTransaction(session -> session.createQuery("FROM User u ORDER BY u.id", User.class).list(),
                "получении списка пользователей");
    }

    @Override
    public User update(User user) {
        return executeInTransaction(session -> {
            User merged = session.merge(user);
            log.info("Обновлён пользователь: {}", merged);
            return merged;
        }, "обновлении пользователя");
    }

    @Override
    public boolean deleteById(Long id) {
        return executeInTransaction(session -> {
            User user = session.get(User.class, id);
            if (user == null) {
                return false;
            }
            session.remove(user);
            log.info("Удалён пользователь с id={}", id);
            return true;
        }, "удалении пользователя");
    }

    private <R> R executeInTransaction(Function<Session, R> action, String operation) {
        Session session = null;
        Transaction tx = null;
        try {
            session = sessionFactory.openSession();
            tx = session.beginTransaction();
            R result = action.apply(session);
            tx.commit();
            return result;
        } catch (ConstraintViolationException e) {
            rollback(tx);
            log.error("Нарушение ограничения БД при {}: {}", operation, e.getMessage());
            throw new DataAccessException("Нарушено ограничение БД (например, email уже занят) при " + operation, e);
        } catch (PersistenceException e) {
            rollback(tx);
            log.error("Ошибка Hibernate при {}", operation, e);
            throw new DataAccessException("Ошибка при " + operation + ": " + e.getMessage(), e);
        } catch (RuntimeException e) {
            rollback(tx);
            if (e.getCause() instanceof SQLException sqlException) {
                log.error("Ошибка SQL при {}", operation, sqlException);
                throw new DataAccessException("Ошибка базы данных при " + operation, sqlException);
            }
            log.error("Непредвиденная ошибка при {}", operation, e);
            throw new DataAccessException("Непредвиденная ошибка при " + operation, e);
        } finally {
            if (session != null) {
                session.close();
            }
        }
    }

    private void rollback(Transaction tx) {
        try {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
        } catch (Exception e) {
            log.error("Не удалось откатить транзакцию", e);
        }
    }
}

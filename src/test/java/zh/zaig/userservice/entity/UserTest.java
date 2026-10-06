package zh.zaig.userservice.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


public class UserTest {

    private static User userWithId(Long id) {
        User user = new User("Sator", "satorarepich@mirror.ru", 48);
        user.setId(id);
        return user;
    }

    @Test
    void equals_whenSameInstance_returnsTrue() {
        User user = userWithId(1L);

        assertEquals(user, user);
    }

    @Test
    void equals_whenSameId_returnsTrueEvenIfOtherFieldsDiffer() {
        User first = userWithId(1L);
        User second = new User("Petr", "petr@mail.com", 40);
        second.setId(1L);

        assertEquals(first, second);
    }

    @Test
    void equals_whenDifferentId_returnsFalse() {
        assertNotEquals(userWithId(1L), userWithId(2L));
    }

    @Test
    void equals_whenNull_returnsFalse() {
        assertNotEquals(null, userWithId(1L));
    }

    @Test
    void equals_whenOtherType_returnsFalse() {
        assertNotEquals("not a user", userWithId(1L));
    }

    @Test
    @DisplayName("Два несохранённых пользователя считаются равными")
    void equals_whenBothIdsNull_returnsTrue() {
        // Фиксируем текущее поведение: equals построен только на id,
        // поэтому у двух новых объектов (id ещё не присвоен) он даёт true
        assertEquals(userWithId(null), userWithId(null));
    }

    @Test
    void equals_whenOneIdNull_returnsFalse() {
        assertNotEquals(userWithId(null), userWithId(1L));
    }

    @Test
    void hashCode_whenSameId_isEqual() {
        assertEquals(userWithId(1L).hashCode(), userWithId(1L).hashCode());
    }

    @Test
    void toString_containsAllFields() {
        User user = userWithId(7L);

        String result = user.toString();

        assertTrue(result.contains("id=7"));
        assertTrue(result.contains("Sator"));
        assertTrue(result.contains("satorarepich@mirror.ru"));
        assertTrue(result.contains("age=48"));
    }

}

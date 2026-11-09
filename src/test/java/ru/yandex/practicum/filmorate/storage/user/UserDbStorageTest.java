package ru.yandex.practicum.filmorate.storage.user;


import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


@JdbcTest
@AutoConfigureTestDatabase
@Import(UserDbStorage.class)
class UserDbStorageTest {

    private final UserDbStorage userStorage;

    @Autowired
    public UserDbStorageTest(UserDbStorage userStorage) {
        this.userStorage = userStorage;
    }

    private User makeUser() {
        User user = new User();
        user.setEmail("ivan@example.com");
        user.setLogin("ivan");
        user.setName("Иван Иванов");
        user.setBirthday(LocalDate.of(1990, 5, 1));
        return user;
    }

    @Test
    void addUserAssignsId() {
        User created = userStorage.addUser(makeUser());

        assertThat(created.getId()).isNotNull().isPositive();
    }

    @Test
    void getUserByIdReturnsSavedUser() {
        User created = userStorage.addUser(makeUser());

        User found = userStorage.getUserById(created.getId());

        assertThat(found).isNotNull();
        assertThat(found)
                .hasFieldOrPropertyWithValue("email", "ivan@example.com")
                .hasFieldOrPropertyWithValue("login", "ivan")
                .hasFieldOrPropertyWithValue("name", "Иван Иванов")
                .hasFieldOrPropertyWithValue("birthday", LocalDate.of(1990, 5, 1));
    }

    @Test
    void getUserByIdReturnsNullForUnknownId() {
        User found = userStorage.getUserById(9999);

        assertThat(found).isNull();
    }

    @Test
    void getAllUsersReturnsCreatedUser() {
        User created = userStorage.addUser(makeUser());

        List<User> users = userStorage.getAllUsers();

        assertThat(users).isNotEmpty();
        assertThat(users)
                .extracting(User::getId)
                .contains(created.getId());
    }

    @Test
    void updateUserChangesFields() {
        User created = userStorage.addUser(makeUser());
        created.setName("Иван Петрович");

        userStorage.updateUser(created);

        User found = userStorage.getUserById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found).hasFieldOrPropertyWithValue("name", "Иван Петрович");
    }

    @Test
    void deleteUserRemovesUser() {
        User created = userStorage.addUser(makeUser());

        userStorage.deleteUser(created.getId());

        assertThat(userStorage.getUserById(created.getId())).isNull();
    }
}

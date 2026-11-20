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
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmDbStorage;

@JdbcTest
@AutoConfigureTestDatabase
@Import({UserDbStorage.class, FilmDbStorage.class})
class UserDbStorageTest {

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;

    @Autowired
    public UserDbStorageTest(UserDbStorage userStorage, FilmDbStorage filmStorage) {
        this.userStorage = userStorage;
        this.filmStorage = filmStorage;
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

    @Test
    void getSimilarUserIdsReturnsOnlyUsersWithMaxOverlap() {
        int targetUser = createUser("target_user_1");
        int mostSimilarUser = createUser("most_similar_user");
        int lessSimilarUser = createUser("less_similar_user");
        Film film1 = createFilm("Film 1");
        Film film2 = createFilm("Film 2");
        Film film3 = createFilm("Film 3");
        filmStorage.addLike(film1.getId(), targetUser);
        filmStorage.addLike(film2.getId(), targetUser);
        filmStorage.addLike(film1.getId(), mostSimilarUser);
        filmStorage.addLike(film2.getId(), mostSimilarUser);
        filmStorage.addLike(film3.getId(), mostSimilarUser);
        filmStorage.addLike(film1.getId(), lessSimilarUser);

        assertThat(userStorage.getSimilarUserIds(targetUser)).containsExactly(mostSimilarUser);
    }

    @Test
    void getSimilarUserIdsReturnsAllUsersOnSameMaximum() {
        int targetUser = createUser("target_user_2");
        int similarUserA = createUser("similar_user_a");
        int similarUserB = createUser("similar_user_b");
        Film film1 = createFilm("Film A");
        Film film2 = createFilm("Film B");
        filmStorage.addLike(film1.getId(), targetUser);
        filmStorage.addLike(film1.getId(), similarUserA);
        filmStorage.addLike(film2.getId(), similarUserA);
        filmStorage.addLike(film1.getId(), similarUserB);
        filmStorage.addLike(film2.getId(), similarUserB);

        assertThat(userStorage.getSimilarUserIds(targetUser))
                .containsExactlyInAnyOrder(similarUserA, similarUserB);
    }

    @Test
    void getSimilarUserIdsEmptyWithTargetNoLikes() {
        int targetUser = createUser("empty_target");
        int otherUser = createUser("other_user");
        Film someFilm = createFilm("Some film");
        filmStorage.addLike(someFilm.getId(), otherUser);

        assertThat(userStorage.getSimilarUserIds(targetUser)).isEmpty();
    }

    private int createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));

        return userStorage.addUser(user).getId();
    }

    private Film createFilm(String name) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        film.setMpa(mpa);

        return filmStorage.addFilm(film);
    }
}

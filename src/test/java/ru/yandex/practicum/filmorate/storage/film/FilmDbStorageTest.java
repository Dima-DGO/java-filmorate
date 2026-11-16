package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.context.annotation.Import;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@Import(FilmDbStorage.class)
class FilmDbStorageTest {

    private final FilmDbStorage filmStorage;
    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public FilmDbStorageTest(FilmDbStorage filmStorage, JdbcTemplate jdbcTemplate) {
        this.filmStorage = filmStorage;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Film makeFilm() {
        Film film = new Film();
        film.setName("Носферату");
        film.setDescription("Немая классика ужасов");
        film.setReleaseDate(LocalDate.of(1922, 3, 4));
        film.setDuration(94);

        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        film.setMpa(mpa);

        Genre g1 = new Genre();
        g1.setId(1);
        Genre g2 = new Genre();
        g2.setId(2);
        film.setGenres(Set.of(g1, g2));
        return film;
    }

    @Test
    void addFilmAssignsIdAndSavesGenres() {
        Film created = filmStorage.addFilm(makeFilm());

        assertThat(created.getId()).isNotNull().isPositive();

        Film found = filmStorage.getFilmById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getMpa()).isNotNull();
        assertThat(found.getMpa().getName()).isNotBlank();
        assertThat(found.getGenres()).hasSize(2);
    }

    @Test
    void getFilmByIdReturnsSavedFilm() {
        Film created = filmStorage.addFilm(makeFilm());

        Film found = filmStorage.getFilmById(created.getId());

        assertThat(found).isNotNull();
        assertThat(found)
                .hasFieldOrPropertyWithValue("name", "Носферату")
                .hasFieldOrPropertyWithValue("duration", 94);
    }

    @Test
    void getFilmByIdReturnsNullForUnknownId() {
        Film found = filmStorage.getFilmById(9999);

        assertThat(found).isNull();
    }

    @Test
    void getAllFilmsReturnsCreatedFilm() {
        Film created = filmStorage.addFilm(makeFilm());

        List<Film> films = filmStorage.getAllFilms();

        assertThat(films).isNotEmpty();
        assertThat(films)
                .extracting(Film::getId)
                .contains(created.getId());
    }

    @Test
    void updateFilmChangesNameAndGenres() {
        Film created = filmStorage.addFilm(makeFilm());

        created.setName("Носферату (обновлённый)");
        Genre g3 = new Genre();
        g3.setId(3);
        created.setGenres(Set.of(g3));

        filmStorage.updateFilm(created);

        Film found = filmStorage.getFilmById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found).hasFieldOrPropertyWithValue("name", "Носферату (обновлённый)");
        assertThat(found.getGenres()).hasSize(1);
        assertThat(found.getGenres().iterator().next().getId()).isEqualTo(3);
    }

    @Test
    void getCommonFilmsReturnsIntersectionSortedByPopularity() {
        int user = createUser("common1");
        int friend = createUser("common2");
        int other = createUser("common3");

        Film lessPopular = filmStorage.addFilm(makeFilm());
        Film morePopular = filmStorage.addFilm(makeFilm());
        Film onlyUser = filmStorage.addFilm(makeFilm());
        Film onlyFriend = filmStorage.addFilm(makeFilm());

        filmStorage.addLike(lessPopular.getId(), user);
        filmStorage.addLike(lessPopular.getId(), friend);
        filmStorage.addLike(morePopular.getId(), user);
        filmStorage.addLike(morePopular.getId(), friend);
        filmStorage.addLike(morePopular.getId(), other);
        filmStorage.addLike(onlyUser.getId(), user);
        filmStorage.addLike(onlyFriend.getId(), friend);

        List<Film> common = filmStorage.getCommonFilms(user, friend);

        assertThat(common)
                .extracting(Film::getId)
                .containsExactly(morePopular.getId(), lessPopular.getId());
        assertThat(common.get(0).getGenres()).hasSize(2);
        assertThat(common.get(0).getMpa()).isNotNull();
    }

    @Test
    void getCommonFilmsReturnsEmptyListWhenNoIntersection() {
        int user = createUser("nocommon1");
        int friend = createUser("nocommon2");

        Film userFilm = filmStorage.addFilm(makeFilm());
        Film friendFilm = filmStorage.addFilm(makeFilm());
        filmStorage.addLike(userFilm.getId(), user);
        filmStorage.addLike(friendFilm.getId(), friend);

        assertThat(filmStorage.getCommonFilms(user, friend)).isEmpty();
    }

    private int createUser(String login) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, login + "@mail.ru");
            ps.setString(2, login);
            ps.setString(3, login);
            ps.setDate(4, Date.valueOf(LocalDate.of(1990, 1, 1)));
            return ps;
        }, keyHolder);
        return keyHolder.getKey().intValue();
    }

    @Test
    void deleteFilmRemovesFilm() {
        Film created = filmStorage.addFilm(makeFilm());

        filmStorage.deleteFilm(created.getId());

        assertThat(filmStorage.getFilmById(created.getId())).isNull();
    }
}

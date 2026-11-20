package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeleteEndpointsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private DirectorService directorService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void deleteFilmRemovesFilmAndItsRelations() throws Exception {
        int author = createUser("del_film_author");
        int reactor = createUser("del_film_reactor");
        Director director = createDirector("Режиссёр удаляемого фильма");
        Film film = createFilm("Удаляемый фильм", director);
        Film otherFilm = createFilm("Остающийся фильм", director);

        filmService.addLike(film.getId(), author);
        filmService.addLike(otherFilm.getId(), author);
        int reviewId = createReview(author, film.getId());
        reviewService.addReaction(reviewId, reactor, true);

        mockMvc.perform(delete("/films/{id}", film.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/{id}", film.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/films/{id}", film.getId()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/reviews/{id}", reviewId))
                .andExpect(status().isNotFound());

        assertThat(countByFilm("likes", film.getId())).isZero();
        assertThat(countByFilm("film_genres", film.getId())).isZero();
        assertThat(countByFilm("film_directors", film.getId())).isZero();
        assertThat(countByFilm("reviews", film.getId())).isZero();
        assertThat(count("SELECT COUNT(*) FROM review_reactions WHERE review_id = ?", reviewId)).isZero();

        mockMvc.perform(get("/films/{id}", otherFilm.getId()))
                .andExpect(status().isOk());
        assertThat(countByFilm("likes", otherFilm.getId())).isEqualTo(1);
        mockMvc.perform(get("/users/{id}", author))
                .andExpect(status().isOk());
        mockMvc.perform(get("/directors/{id}", director.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteFilmReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/films/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUserRemovesUserAndItsRelations() throws Exception {
        int user = createUser("del_user");
        int friend = createUser("del_user_friend");
        int follower = createUser("del_user_follower");
        Film film = createFilm("Фильм для удаления пользователя", null);

        userService.addFriend(user, friend);
        userService.addFriend(follower, user);
        filmService.addLike(film.getId(), user);
        filmService.addLike(film.getId(), friend);

        int ownReviewId = createReview(user, film.getId());
        reviewService.addReaction(ownReviewId, friend, true);
        int friendReviewId = createReview(friend, film.getId());
        reviewService.addReaction(friendReviewId, user, true);

        mockMvc.perform(delete("/users/{id}", user))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{id}", user))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/users/{id}", user))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/reviews/{id}", ownReviewId))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/reviews/{id}", friendReviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useful").value(0));
        mockMvc.perform(get("/users/{id}/friends", follower))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        assertThat(count("SELECT COUNT(*) FROM likes WHERE user_id = ?", user)).isZero();
        assertThat(countByFilm("likes", film.getId())).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM friendships WHERE user_id = ? OR friend_id = ?", user, user))
                .isZero();
        assertThat(count("SELECT COUNT(*) FROM reviews WHERE user_id = ?", user)).isZero();
        assertThat(count("SELECT COUNT(*) FROM review_reactions WHERE user_id = ?", user)).isZero();

        mockMvc.perform(get("/users/{id}", friend))
                .andExpect(status().isOk());
        mockMvc.perform(get("/films/{id}", film.getId()))
                .andExpect(status().isOk());
    }

    @Test
    void deleteUserReturnsNotFoundForUnknownId() throws Exception {
        mockMvc.perform(delete("/users/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    private int countByFilm(String table, int filmId) {
        return count("SELECT COUNT(*) FROM " + table + " WHERE film_id = ?", filmId);
    }

    private int count(String sql, Object... args) {
        Integer result = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return result == null ? 0 : result;
    }

    private int createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.addUser(user).getId();
    }

    private Director createDirector(String name) {
        Director director = new Director();
        director.setName(name);
        return directorService.addDirector(director);
    }

    private Film createFilm(String name, Director director) {
        Film film = new Film();
        film.setName(name);
        film.setDescription("Описание");
        film.setReleaseDate(LocalDate.of(2000, 1, 1));
        film.setDuration(100);
        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        film.setMpa(mpa);
        Genre genre = new Genre();
        genre.setId(1);
        film.setGenres(Set.of(genre));
        if (director != null) {
            film.setDirectors(Set.of(director));
        }
        return filmService.addFilm(film);
    }

    private int createReview(int userId, int filmId) {
        Review review = new Review();
        review.setContent("Отзыв");
        review.setPositive(true);
        review.setUserId(userId);
        review.setFilmId(filmId);
        return reviewService.addReview(review).getReviewId();
    }
}

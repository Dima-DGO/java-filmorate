package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.DirectorService;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RecommendationApiTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    @Autowired
    private DirectorService directorService;

    @Test
    void recommendsFilmsLikedByMostSimilarUser() throws Exception {
        int target = createUser("target");
        int mostLike = createUser("mostLike");
        int lessLike = createUser("lessLike");
        Film shared1 = createFilm("Shared 1");
        Film shared2 = createFilm("Shared 2");
        Film recommended = createFilm("Recommended");
        Film notRecommended = createFilm("Not recommended");

        like(shared1, target);
        like(shared2, target);

        like(shared1, mostLike);
        like(shared2, mostLike);
        like(recommended, mostLike);

        like(shared1, lessLike);
        like(notRecommended, lessLike);

        mockMvc.perform(get("/users/{id}/recommendations", target)
                .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(recommended.getId()));
    }

    @Test
    void returnsUnionOfRecommendationsOfAllTopUsers() throws Exception {
        int target = createUser("tie_target");
        int simA = createUser("tie_simA");
        int simB = createUser("tie_simB");
        Film common = createFilm("Common Film");
        Film recA = createFilm("Rec from A");
        Film recB = createFilm("Rec from B");

        like(common, target);
        like(common, simA);
        like(recA, simA);
        like(common, simB);
        like(recB, simB);

        mockMvc.perform(get("/users/{id}/recommendations", target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[?(@.id == " + recA.getId() + ")]").exists())
                .andExpect(jsonPath("$[?(@.id == " + recB.getId() + ")]").exists());
    }

    @Test
    void excludesFilmsAlreadyLikedByTargetUserWithoutDuplicates() throws Exception {
        int target = createUser("dup_target");
        int simA = createUser("dup_simA");
        int simB = createUser("dup_simB");
        Film alreadyLiked = createFilm("Already Liked");
        Film sharedRecommendation = createFilm("sharedRecommendation");

        like(alreadyLiked, target);
        like(alreadyLiked, simA);
        like(sharedRecommendation, simA);
        like(alreadyLiked, simB);
        like(sharedRecommendation, simB);

        mockMvc.perform(get("/users/{id}/recommendations", target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(sharedRecommendation.getId()));
    }

    @Test
    void returnsBadRequestForInvalidUserIdType() throws Exception {
        mockMvc.perform(get("/users/{id}/recommendations", "not-a-number"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void returnEmptyListWhenTargetUserWithNoLikes() throws Exception {
        int target = createUser("no_likes_target");
        int other = createUser("no_likes_other");
        Film film = createFilm("Some Film");

        like(film, other);

        mockMvc.perform(get("/users/{id}/recommendations", target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsEmptyListWhenNoSimilarUsersFound() throws Exception {
        int target = createUser("lonely_target");
        int other = createUser("lonely_other");

        Film targetFilm = createFilm("Target Film");
        Film otherFilm = createFilm("Other Film");

        like(targetFilm, target);
        like(otherFilm, other);

        mockMvc.perform(get("/users/{id}/recommendations", target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void recommendationsWithGenresAndDirectors() throws Exception {
        int target = createUser("format_target");
        int similar = createUser("format_similar");
        Film base = createFilm("Base Film");
        int directorId = createDirector("Test Director");
        Film rec = createFilm("Rich Rec Film");
        Genre genre = new Genre();
        genre.setId(1);
        rec.setGenres(new LinkedHashSet<>(Set.of(genre)));
        Director director = new Director();
        director.setId(directorId);
        rec.setDirectors(new LinkedHashSet<>(Set.of(director)));
        filmService.updateFilm(rec);

        like(base, target);
        like(base, similar);
        like(rec, similar);

        mockMvc.perform(get("/users/{id}/recommendations", target))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(rec.getId()))
                .andExpect(jsonPath("$[0].genres", hasSize(1)))
                .andExpect(jsonPath("$[0].genres[0].id").value(1))
                .andExpect(jsonPath("$[0].directors", hasSize(1)))
                .andExpect(jsonPath("$[0].directors[0].id").value(directorId));
    }

    @Test
    void returnsNotFoundForNonexistentUser() throws Exception {
        mockMvc.perform(get("/users/{id}/recommendations", 999_999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    private int createUser(String login) {
        User user = new User();
        user.setEmail(login + "@mail.ru");
        user.setLogin(login);
        user.setName(login);
        user.setBirthday(LocalDate.of(1990, 1, 1));
        return userService.addUser(user).getId();
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
        return filmService.addFilm(film);
    }

    private void like(Film film, int userId) {
        filmService.addLike(film.getId(), userId);
    }

    private int createDirector(String name) {
        Director director = new Director();
        director.setName(name);
        return directorService.addDirector(director).getId();
    }
}
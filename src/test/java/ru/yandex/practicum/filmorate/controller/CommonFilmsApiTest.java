package ru.yandex.practicum.filmorate.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CommonFilmsApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    @Test
    void returnsCommonFilmsSortedByPopularity() throws Exception {
        int user = createUser("api_user");
        int friend = createUser("api_friend");
        int other = createUser("api_other");

        Film first = createFilm("Первый");
        Film second = createFilm("Второй");
        Film notCommon = createFilm("Не общий");

        filmService.addLike(first.getId(), user);
        filmService.addLike(first.getId(), friend);
        filmService.addLike(second.getId(), user);
        filmService.addLike(second.getId(), friend);
        filmService.addLike(second.getId(), other);
        filmService.addLike(notCommon.getId(), user);

        mockMvc.perform(get("/films/common")
                        .param("userId", String.valueOf(user))
                        .param("friendId", String.valueOf(friend))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(second.getId()))
                .andExpect(jsonPath("$[1].id").value(first.getId()));
    }

    @Test
    void returnsEmptyListWhenNoCommonFilms() throws Exception {
        int user = createUser("api_empty1");
        int friend = createUser("api_empty2");

        Film film = createFilm("Только у одного");
        filmService.addLike(film.getId(), user);

        mockMvc.perform(get("/films/common")
                        .param("userId", String.valueOf(user))
                        .param("friendId", String.valueOf(friend)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void returnsNotFoundForUnknownUser() throws Exception {
        int user = createUser("api_known");

        mockMvc.perform(get("/films/common")
                        .param("userId", String.valueOf(user))
                        .param("friendId", "999999"))
                .andExpect(status().isNotFound());
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
}

package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FilmDirectorConsistencyTest {

    private static final int UNKNOWN_DIRECTOR_ID = 999_999;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private int createDirector(String name) throws Exception {
        String body = mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asInt();
    }

    private int createUser(String login) throws Exception {
        String body = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + login + "@mail.ru\",\"login\":\"" + login
                                + "\",\"birthday\":\"1990-01-01\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asInt();
    }

    private String filmJson(String name, int directorId) {
        return "{\"name\":\"" + name + "\",\"releaseDate\":\"2000-01-01\",\"duration\":100,"
                + "\"mpa\":{\"id\":1},\"director\":[{\"id\":" + directorId + "}]}";
    }

    private int createFilm(String name, int directorId) throws Exception {
        String body = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson(name, directorId)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asInt();
    }

    @Test
    void filmIsNotSavedWhenDirectorDoesNotExist() throws Exception {
        String name = "Orphan film " + System.nanoTime();

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(filmJson(name, UNKNOWN_DIRECTOR_ID)))
                .andExpect(status().isNotFound());

        String all = mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(all).doesNotContain(name);
    }

    @Test
    void directorsAreKeptWhenUpdateWithUnknownDirectorFails() throws Exception {
        int directorId = createDirector("Consistency director " + System.nanoTime());
        int filmId = createFilm("Consistency film " + System.nanoTime(), directorId);

        String updateBody = "{\"id\":" + filmId + ",\"name\":\"Changed name\","
                + "\"releaseDate\":\"2000-01-01\",\"duration\":100,\"mpa\":{\"id\":1},"
                + "\"director\":[{\"id\":" + UNKNOWN_DIRECTOR_ID + "}]}";

        mockMvc.perform(put("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isNotFound());

        String body = mockMvc.perform(get("/films/{id}", filmId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode directors = objectMapper.readTree(body).get("directors");
        assertThat(directors.size()).isEqualTo(1);
        assertThat(directors.get(0).get("id").asInt()).isEqualTo(directorId);
    }

    @Test
    void commonFilmsContainDirectors() throws Exception {
        int directorId = createDirector("Common director " + System.nanoTime());
        int filmId = createFilm("Common film " + System.nanoTime(), directorId);
        int firstUser = createUser("commonuser1" + System.nanoTime());
        int secondUser = createUser("commonuser2" + System.nanoTime());

        mockMvc.perform(put("/films/{filmId}/like/{userId}", filmId, firstUser))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{filmId}/like/{userId}", filmId, secondUser))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/films/common")
                        .param("userId", String.valueOf(firstUser))
                        .param("friendId", String.valueOf(secondUser)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode found = null;
        for (JsonNode film : objectMapper.readTree(body)) {
            if (film.get("id").asInt() == filmId) {
                found = film;
            }
        }

        assertThat(found).isNotNull();
        assertThat(found.get("directors").size()).isEqualTo(1);
        assertThat(found.get("directors").get(0).get("id").asInt()).isEqualTo(directorId);
    }
}

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

    private JsonNode findFilm(JsonNode films, int filmId) {
        for (JsonNode film : films) {
            if (film.get("id").asInt() == filmId) {
                return film;
            }
        }
        return null;
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

    @Test
    void filmIsNotSavedWhenGenreDoesNotExist() throws Exception {
        String name = "Genre orphan " + System.nanoTime();
        String body = "{\"name\":\"" + name + "\",\"releaseDate\":\"2000-01-01\",\"duration\":100,"
                + "\"mpa\":{\"id\":1},\"genres\":[{\"id\":999999}]}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        String all = mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(all).doesNotContain(name);
    }

    @Test
    void popularFilmsContainDirectors() throws Exception {
        int directorId = createDirector("Popular director " + System.nanoTime());
        int filmId = createFilm("Popular film " + System.nanoTime(), directorId);
        int userId = createUser("popularuser" + System.nanoTime());

        mockMvc.perform(put("/films/{filmId}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/films/popular").param("count", "1000"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode found = findFilm(objectMapper.readTree(body), filmId);
        assertThat(found).isNotNull();
        assertThat(found.get("directors").size()).isEqualTo(1);
        assertThat(found.get("directors").get(0).get("id").asInt()).isEqualTo(directorId);
    }

    @Test
    void filmsByDirectorContainDirectors() throws Exception {
        int directorId = createDirector("Endpoint director " + System.nanoTime());
        int filmId = createFilm("Endpoint film " + System.nanoTime(), directorId);

        String body = mockMvc.perform(get("/films/director/{id}", directorId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode found = findFilm(objectMapper.readTree(body), filmId);
        assertThat(found).isNotNull();
        assertThat(found.get("directors").size()).isEqualTo(1);
        assertThat(found.get("directors").get(0).get("id").asInt()).isEqualTo(directorId);
    }

    @Test
    void filmIsNotSavedWhenDirectorHasNoId() throws Exception {
        String name = "Director without id " + System.nanoTime();
        String body = "{\"name\":\"" + name + "\",\"releaseDate\":\"2000-01-01\",\"duration\":100,"
                + "\"mpa\":{\"id\":1},\"director\":[{\"name\":\"Unknown director\"}]}";

        mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());

        String all = mockMvc.perform(get("/films"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(all).doesNotContain(name);
    }

    @Test
    void directorsAreEmptyAfterDirectorDeleted() throws Exception {
        int directorId = createDirector("Deletable director " + System.nanoTime());
        int filmId = createFilm("Film of deleted director " + System.nanoTime(), directorId);

        mockMvc.perform(delete("/directors/{id}", directorId))
                .andExpect(status().isOk());

        String body = mockMvc.perform(get("/films/{id}", filmId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode directors = objectMapper.readTree(body).get("directors");
        assertThat(directors).isNotNull();
        assertThat(directors.size()).isEqualTo(0);
    }

    @Test
    void filmWithoutDirectorsHasEmptyDirectorsField() throws Exception {
        String name = "Film without directors " + System.nanoTime();
        String body = "{\"name\":\"" + name + "\",\"releaseDate\":\"2000-01-01\",\"duration\":100,"
                + "\"mpa\":{\"id\":1}}";

        String created = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int filmId = objectMapper.readTree(created).get("id").asInt();

        String film = mockMvc.perform(get("/films/{id}", filmId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        JsonNode node = objectMapper.readTree(film);
        assertThat(node.has("directors")).isTrue();
        assertThat(node.get("directors").size()).isEqualTo(0);
    }
}

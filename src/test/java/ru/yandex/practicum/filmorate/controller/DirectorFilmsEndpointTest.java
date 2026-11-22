package ru.yandex.practicum.filmorate.controller;

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
class DirectorFilmsEndpointTest {

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

    private int createFilm(String name, int year, int directorId) throws Exception {
        String body = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"releaseDate\":\"" + year + "-01-01\","
                                + "\"duration\":100,\"mpa\":{\"id\":1},\"director\":[{\"id\":" + directorId + "}]}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asInt();
    }

    @Test
    void filmsSortedByYearAscending() throws Exception {
        int directorId = createDirector("Sort Year Director");
        int late = createFilm("Late Film", 2010, directorId);
        int early = createFilm("Early Film", 1999, directorId);

        String body = mockMvc.perform(get("/films/director/{id}", directorId)
                        .param("sortBy", "year"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int firstId = objectMapper.readTree(body).get(0).get("id").asInt();
        int secondId = objectMapper.readTree(body).get(1).get("id").asInt();
        assertThat(firstId).isEqualTo(early);
        assertThat(secondId).isEqualTo(late);
    }

    @Test
    void filmsSortedByLikesDescending() throws Exception {
        int directorId = createDirector("Sort Likes Director");
        int popular = createFilm("Popular Film", 2001, directorId);
        int unpopular = createFilm("Unpopular Film", 2002, directorId);
        int user1 = createUser("likes_user_1");
        int user2 = createUser("likes_user_2");

        mockMvc.perform(put("/films/{f}/like/{u}", popular, user1)).andExpect(status().isOk());
        mockMvc.perform(put("/films/{f}/like/{u}", popular, user2)).andExpect(status().isOk());
        mockMvc.perform(put("/films/{f}/like/{u}", unpopular, user1)).andExpect(status().isOk());

        String body = mockMvc.perform(get("/films/director/{id}", directorId)
                        .param("sortBy", "likes"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        int firstId = objectMapper.readTree(body).get(0).get("id").asInt();
        assertThat(firstId).isEqualTo(popular);
    }

    @Test
    void sortByDefaultsToLikes() throws Exception {
        int directorId = createDirector("Default Sort Director");
        createFilm("Only Film", 2005, directorId);

        mockMvc.perform(get("/films/director/{id}", directorId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].directors").isArray());
    }

    @Test
    void invalidSortByReturns400() throws Exception {
        int directorId = createDirector("Bad Sort Director");

        mockMvc.perform(get("/films/director/{id}", directorId)
                        .param("sortBy", "rating"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void unknownDirectorReturns404() throws Exception {
        mockMvc.perform(get("/films/director/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}

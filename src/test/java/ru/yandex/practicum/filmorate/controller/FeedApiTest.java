package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:feed-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class FeedApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM events");
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("Лента возвращает семь событий в порядке совершения действий")
    void shouldReturnAllEventsInOrder() throws Exception {
        int userId = createUser("feed-author");
        int friendId = createUser("feed-friend");
        int filmId = createFilm("Feed film");

        mockMvc.perform(put("/users/{id}/friends/{friendId}", userId, friendId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/users/{id}/friends/{friendId}", userId, friendId))
                .andExpect(status().isOk());

        int reviewId = createReview(userId, filmId);
        updateReview(reviewId, userId, filmId);
        mockMvc.perform(delete("/reviews/{id}", reviewId)).andExpect(status().isOk());

        mockMvc.perform(put("/films/{filmId}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/films/{filmId}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{userId}/feed", userId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(7))
                .andExpect(jsonPath("$[0].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[0].entityId").value(friendId))
                .andExpect(jsonPath("$[0].eventId").isNumber())
                .andExpect(jsonPath("$[0].timestamp").value(greaterThan(1670590017281L)))
                .andExpect(jsonPath("$[1].eventType").value("FRIEND"))
                .andExpect(jsonPath("$[1].operation").value("REMOVE"))
                .andExpect(jsonPath("$[1].entityId").value(friendId))
                .andExpect(jsonPath("$[2].eventType").value("REVIEW"))
                .andExpect(jsonPath("$[2].operation").value("ADD"))
                .andExpect(jsonPath("$[2].entityId").value(reviewId))
                .andExpect(jsonPath("$[3].eventType").value("REVIEW"))
                .andExpect(jsonPath("$[3].operation").value("UPDATE"))
                .andExpect(jsonPath("$[3].entityId").value(reviewId))
                .andExpect(jsonPath("$[4].eventType").value("REVIEW"))
                .andExpect(jsonPath("$[4].operation").value("REMOVE"))
                .andExpect(jsonPath("$[4].entityId").value(reviewId))
                .andExpect(jsonPath("$[5].eventType").value("LIKE"))
                .andExpect(jsonPath("$[5].operation").value("ADD"))
                .andExpect(jsonPath("$[5].entityId").value(filmId))
                .andExpect(jsonPath("$[6].eventType").value("LIKE"))
                .andExpect(jsonPath("$[6].operation").value("REMOVE"))
                .andExpect(jsonPath("$[6].entityId").value(filmId));
    }

    @Test
    @DisplayName("У пользователя без действий лента пустая")
    void shouldReturnEmptyFeed() throws Exception {
        int userId = createUser("feed-empty");

        mockMvc.perform(get("/users/{userId}/feed", userId))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Лента несуществующего пользователя возвращает 404")
    void shouldReturnNotFoundForUnknownUser() throws Exception {
        mockMvc.perform(get("/users/{userId}/feed", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Повторный лайк записывается отдельным событием")
    void shouldWriteEventForRepeatedLike() throws Exception {
        int userId = createUser("feed-liker");
        int filmId = createFilm("Feed like film");

        mockMvc.perform(put("/films/{filmId}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{filmId}/like/{userId}", filmId, userId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{userId}/feed", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].eventType").value("LIKE"))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[0].entityId").value(filmId))
                .andExpect(jsonPath("$[1].eventType").value("LIKE"))
                .andExpect(jsonPath("$[1].operation").value("ADD"))
                .andExpect(jsonPath("$[1].entityId").value(filmId));
    }

    @Test
    @DisplayName("Удаление отзыва не стирает его события из ленты")
    void shouldKeepReviewEventsAfterReviewDeleted() throws Exception {
        int userId = createUser("feed-reviewer");
        int filmId = createFilm("Feed review film");

        int reviewId = createReview(userId, filmId);
        updateReview(reviewId, userId, filmId);
        mockMvc.perform(delete("/reviews/{id}", reviewId)).andExpect(status().isOk());

        mockMvc.perform(get("/users/{userId}/feed", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].operation").value("ADD"))
                .andExpect(jsonPath("$[1].operation").value("UPDATE"))
                .andExpect(jsonPath("$[2].operation").value("REMOVE"))
                .andExpect(jsonPath("$[0].entityId").value(reviewId))
                .andExpect(jsonPath("$[2].entityId").value(reviewId));
    }

    @Test
    @DisplayName("В ленту не попадают события других пользователей")
    void shouldNotMixEventsOfOtherUsers() throws Exception {
        int firstUserId = createUser("feed-first");
        int secondUserId = createUser("feed-second");
        int firstFilmId = createFilm("First film");
        int secondFilmId = createFilm("Second film");

        mockMvc.perform(put("/films/{filmId}/like/{userId}", firstFilmId, firstUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{filmId}/like/{userId}", secondFilmId, secondUserId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/users/{userId}/feed", firstUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(firstUserId))
                .andExpect(jsonPath("$[0].entityId").value(firstFilmId));

        mockMvc.perform(get("/users/{userId}/feed", secondUserId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].entityId").value(secondFilmId));
    }

    private int createUser(String login) throws Exception {
        ObjectNode request = objectMapper.createObjectNode()
                .put("login", login)
                .put("email", login + "@example.com")
                .put("birthday", "1990-05-15");
        String response = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    private int createFilm(String name) throws Exception {
        ObjectNode request = objectMapper.createObjectNode()
                .put("name", name)
                .put("description", "Описание для теста ленты")
                .put("releaseDate", "2014-11-06")
                .put("duration", 120);
        request.putObject("mpa").put("id", 1);
        request.putArray("genres").addObject().put("id", 1);
        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    private int createReview(int userId, int filmId) throws Exception {
        ObjectNode request = objectMapper.createObjectNode()
                .put("content", "Плохой фильм")
                .put("isPositive", false)
                .put("userId", userId)
                .put("filmId", filmId);
        String response = mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("reviewId").asInt();
    }

    private void updateReview(int reviewId, int userId, int filmId) throws Exception {
        ObjectNode request = objectMapper.createObjectNode()
                .put("reviewId", reviewId)
                .put("content", "Теперь фильм неплох")
                .put("isPositive", true)
                .put("userId", userId)
                .put("filmId", filmId);
        mockMvc.perform(put("/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk());
    }
}

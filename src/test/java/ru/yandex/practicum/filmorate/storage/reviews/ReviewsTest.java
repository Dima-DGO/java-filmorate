package ru.yandex.practicum.filmorate.storage.reviews;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:reviews-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
public class ReviewsTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    private int userId;
    private int filmId;

    @BeforeEach
    void beforeEach() throws Exception {
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
        userId = createUser("ivan");
        filmId = createFilm("Интерстеллар");
    }

    @Test
    @DisplayName("При отсутствии отзывов возвращается пустой JSON-массив")
    void shouldReturnAllReviews() throws Exception {
        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("Созданный отзыв сохраняется в базе и возвращается по ID")
    void shouldCreateAndGetReview() throws Exception {
        ObjectNode request = reviewBody();
        request.put("reviewId", 99999);
        request.put("useful", 100);
        JsonNode review = read(mockMvc.perform(post("/reviews")
                        .contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isOk()));
        int id = review.get("reviewId").asInt();
        assertTrue(id > 0 && id != 99999);
        assertEquals(0, review.get("useful").asInt());
        assertEquals(6, review.size());
        mockMvc.perform(get("/reviews/{id}", id))
                .andExpect(status().isOk())
                .andExpect(content().json(review.toString()));
        assertEquals("Хороший фильм", jdbcTemplate.queryForObject(
                "SELECT content FROM reviews WHERE review_id = ?", String.class, id));
        mockMvc.perform(get("/reviews"))
                .andExpect(jsonPath("$[0].reviewId").value(id));
    }

    @Test
    @DisplayName("Обновление меняет текст и тип отзыва, сохраняя автора, фильм и полезность")
    void shouldUpdateReviewWithoutChangingAuthorOrFilm() throws Exception {
        int id = createReview();
        ObjectNode request = reviewBody();
        request.put("reviewId", id);
        request.put("content", "Передумал");
        request.put("isPositive", false);
        request.put("userId", createUser("alex"));
        request.put("filmId", createFilm("Другой фильм"));
        request.put("useful", 100);
        mockMvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("Передумал"))
                .andExpect(jsonPath("$.isPositive").value(false))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.filmId").value(filmId))
                .andExpect(jsonPath("$.useful").value(0));
        mockMvc.perform(get("/reviews/{id}", id))
                .andExpect(jsonPath("$.content").value("Передумал"));
    }

    @Test
    @DisplayName("Удалённый отзыв больше не находится по ID и отсутствует в списке")
    void shouldDeleteReview() throws Exception {
        int id = createReview();
        mockMvc.perform(delete("/reviews/{id}", id)).andExpect(status().isOk());
        mockMvc.perform(get("/reviews/{id}", id)).andExpect(status().isNotFound());
        mockMvc.perform(get("/reviews")).andExpect(content().json("[]"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"content", "isPositive", "userId", "filmId"})
    @DisplayName("Создание отзыва без обязательного поля возвращает 400")
    void shouldRejectMissingRequiredField(String field) throws Exception {
        ObjectNode request = reviewBody();
        request.remove(field);
        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Пустой текст отзыва возвращает 400")
    void shouldRejectBlankContent() throws Exception {
        ObjectNode request = reviewBody().put("content", "   ");
        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
    }

    @ParameterizedTest
    @ValueSource(strings = {"userId", "filmId"})
    @DisplayName("Создание отзыва для несуществующего пользователя или фильма возвращает 404")
    void shouldRejectUnknownUserOrFilm(String field) throws Exception {
        ObjectNode request = reviewBody().put(field, -1);
        mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Чтение, обновление и удаление несуществующего отзыва возвращают 404")
    void shouldRejectUnknownReview() throws Exception {
        mockMvc.perform(get("/reviews/99999")).andExpect(status().isNotFound());
        mockMvc.perform(delete("/reviews/99999")).andExpect(status().isNotFound());
        ObjectNode request = reviewBody().put("reviewId", 99999);
        mockMvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Обновление без ID или с некорректным содержимым возвращает 400")
    void shouldValidateUpdate() throws Exception {
        mockMvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content(reviewBody().toString()))
                .andExpect(status().isBadRequest());
        ObjectNode request = reviewBody().put("reviewId", createReview()).put("content", "");
        mockMvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isBadRequest());
    }


    @Test
    @DisplayName("Повторный лайк не увеличивает полезность, смена на дизлайк даёт минус один")
    void shouldReplaceReactionWithoutCountingDuplicates() throws Exception {
        int id = createReview();
        react("like", id, userId);
        react("like", id, userId);
        assertUseful(id, 1);
        react("dislike", id, userId);
        react("dislike", id, userId);
        assertUseful(id, -1);
        react("like", id, userId);
        assertUseful(id, 1);
    }

    @Test
    @DisplayName("Оценки разных пользователей суммируются")
    void shouldSumDifferentUsersReactions() throws Exception {
        int id = createReview();
        react("like", id, userId);
        react("like", id, createUser("alex"));
        react("dislike", id, createUser("kate"));
        assertUseful(id, 1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "dislike"})
    @DisplayName("Удаление оценки возвращает полезность к нулю, повторное удаление безопасно")
    void shouldRemoveReaction(String type) throws Exception {
        int id = createReview();
        react(type, id, userId);
        mockMvc.perform(delete("/reviews/{id}/{type}/{userId}", id, type, userId))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/reviews/{id}/{type}/{userId}", id, type, userId))
                .andExpect(status().isOk());
        assertUseful(id, 0);
    }

    @Test
    @DisplayName("Удаление лайка не удаляет дизлайк и наоборот")
    void shouldNotRemoveOppositeReaction() throws Exception {
        int id = createReview();
        react("like", id, userId);
        mockMvc.perform(delete("/reviews/{id}/dislike/{userId}", id, userId))
                .andExpect(status().isOk());
        assertUseful(id, 1);
        react("dislike", id, userId);
        mockMvc.perform(delete("/reviews/{id}/like/{userId}", id, userId))
                .andExpect(status().isOk());
        assertUseful(id, -1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"like", "dislike"})
    @DisplayName("Оценки для несуществующего отзыва или пользователя возвращают 404")
    void shouldRejectReactionForUnknownEntities(String type) throws Exception {
        int id = createReview();
        mockMvc.perform(put("/reviews/99999/{type}/{userId}", type, userId))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/reviews/{id}/{type}/99999", id, type))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/reviews/99999/{type}/{userId}", type, userId))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/reviews/{id}/{type}/99999", id, type))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Список сортируется по полезности, фильтруется по фильму и ограничивается count")
    void shouldFilterSortAndLimitReviews() throws Exception {
        int first = createReview();
        int second = createReview();
        int originalFilmId = filmId;
        filmId = createFilm("Другой фильм");
        int other = createReview();
        react("dislike", first, userId);
        react("like", second, userId);
        react("like", other, userId);
        react("like", other, createUser("alex"));
        mockMvc.perform(get("/reviews"))
                .andExpect(jsonPath("$[0].reviewId").value(other))
                .andExpect(jsonPath("$[1].reviewId").value(second))
                .andExpect(jsonPath("$[2].reviewId").value(first));
        mockMvc.perform(get("/reviews").param("filmId", String.valueOf(originalFilmId)).param("count", "1"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].reviewId").value(second));
        mockMvc.perform(get("/reviews").param("filmId", "99999"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("По умолчанию возвращается десять отзывов, count может изменить предел")
    void shouldApplyDefaultAndExplicitCount() throws Exception {
        for (int i = 0; i < 12; i++) {
            createReview();
        }
        mockMvc.perform(get("/reviews")).andExpect(jsonPath("$.length()").value(10));
        mockMvc.perform(get("/reviews").param("count", "11")).andExpect(jsonPath("$.length()").value(11));
        mockMvc.perform(get("/reviews").param("count", "0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/reviews").param("count", "-1")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Обновление отзыва сохраняет набранную полезность")
    void shouldKeepUsefulWhenUpdatingReview() throws Exception {
        int id = createReview();
        react("like", id, userId);
        ObjectNode request = reviewBody().put("reviewId", id).put("content", "Новый текст").put("useful", 999);
        mockMvc.perform(put("/reviews").contentType(MediaType.APPLICATION_JSON).content(request.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.useful").value(1));
        assertUseful(id, 1);
    }

    @Test
    @DisplayName("При удалении отзыва удаляются его оценки")
    void shouldDeleteReviewsReactions() throws Exception {
        int id = createReview();
        react("like", id, userId);
        mockMvc.perform(delete("/reviews/{id}", id)).andExpect(status().isOk());
        assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM review_reactions WHERE review_id = ?", Integer.class, id));
    }

    private void react(String type, int id, int authorId) throws Exception {
        mockMvc.perform(put("/reviews/{id}/{type}/{userId}", id, type, authorId))
                .andExpect(status().isOk());
    }

    private void assertUseful(int id, int useful) throws Exception {
        mockMvc.perform(get("/reviews/{id}", id))
                .andExpect(status().isOk()).andExpect(jsonPath("$.useful").value(useful));
    }

    private ObjectNode reviewBody() {
        return objectMapper.createObjectNode()
                .put("content", "Хороший фильм").put("isPositive", true)
                .put("userId", userId).put("filmId", filmId);
    }

    private int createReview() throws Exception {
        return read(mockMvc.perform(post("/reviews").contentType(MediaType.APPLICATION_JSON)
                .content(reviewBody().toString())).andExpect(status().isOk()))
                .get("reviewId").asInt();
    }

    private int createUser(String login) throws Exception {
        ObjectNode request = objectMapper.createObjectNode().put("login", login)
                .put("email", login + "@example.com").put("birthday", "1990-05-15");
        return read(mockMvc.perform(post("/users").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString())).andExpect(status().isOk())).get("id").asInt();
    }

    private int createFilm(String name) throws Exception {
        ObjectNode request = objectMapper.createObjectNode().put("name", name)
                .put("description", "Описание").put("releaseDate", "2014-11-06").put("duration", 169);
        request.putObject("mpa").put("id", 1);
        return read(mockMvc.perform(post("/films").contentType(MediaType.APPLICATION_JSON)
                .content(request.toString())).andExpect(status().isOk())).get("id").asInt();
    }

    private JsonNode read(ResultActions result) throws Exception {
        return objectMapper.readTree(result.andReturn().getResponse().getContentAsString());
    }
}

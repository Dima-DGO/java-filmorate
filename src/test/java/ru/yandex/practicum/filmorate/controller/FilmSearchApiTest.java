package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.JsonNode;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:film-search-tests;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
class FilmSearchApiTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void beforeEach() {
        jdbcTemplate.update("DELETE FROM films");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    @DisplayName("Поиск по части названия без учёта регистра возвращает только подходящие фильмы")
    void shouldFindFilmsByTitleIgnoringCase() throws Exception {
        JsonNode matchingFilm = createFilm("The Matrix", "История о выборе");
        createFilm("Interstellar", "The Matrix");

        mockMvc.perform(get("/films/search")
                        .param("query", "mAtR")
                        .param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[" + matchingFilm + "]"));
    }

    @Test
    @DisplayName("Поиск возвращает фильмы по убыванию лайков, включая фильмы без лайков")
    void shouldSortSearchResultsByPopularity() throws Exception {
        int withoutLikes = createFilm("Matrix Zero", "Без лайков").get("id").asInt();
        int withOneLike = createFilm("Matrix One", "Один лайк").get("id").asInt();
        int withTwoLikes = createFilm("Matrix Two", "Два лайка").get("id").asInt();
        int firstUserId = createUser("first");
        int secondUserId = createUser("second");

        mockMvc.perform(put("/films/{filmId}/like/{userId}", withOneLike, firstUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{filmId}/like/{userId}", withTwoLikes, firstUserId))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{filmId}/like/{userId}", withTwoLikes, secondUserId))
                .andExpect(status().isOk());

        mockMvc.perform(get("/films/search")
                        .param("query", "matrix")
                        .param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(withTwoLikes))
                .andExpect(jsonPath("$[1].id").value(withOneLike))
                .andExpect(jsonPath("$[2].id").value(withoutLikes));
    }

    @Test
    @DisplayName("Поиск без совпадений возвращает пустой JSON-массив")
    void shouldReturnEmptyArrayWhenNoTitlesMatch() throws Exception {
        createFilm("The Matrix", "История о выборе");
        createFilm("Interstellar", "Космическое путешествие");

        mockMvc.perform(get("/films/search")
                        .param("query", "Avatar")
                        .param("by", "title"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]"));
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

    private JsonNode createFilm(String name, String description) throws Exception {
        ObjectNode request = objectMapper.createObjectNode()
                .put("name", name)
                .put("description", description)
                .put("releaseDate", "2014-11-06")
                .put("duration", 120);
        request.putObject("mpa").put("id", 1);
        request.putArray("genres").addObject().put("id", 1);

        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request.toString()))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }
}

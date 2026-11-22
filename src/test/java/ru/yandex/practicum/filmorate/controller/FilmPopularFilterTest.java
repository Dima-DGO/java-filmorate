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
class FilmPopularFilterTest {

    private static final int BIG_COUNT = 1000;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private int createFilm(String name, String releaseDate, String genresJson) throws Exception {
        String body = "{\"name\":\"" + name + "\",\"releaseDate\":\"" + releaseDate + "\","
                + "\"duration\":100,\"mpa\":{\"id\":1}" + genresJson + "}";
        String response = mockMvc.perform(post("/films")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    private JsonNode getPopular(String query) throws Exception {
        String body = mockMvc.perform(get("/films/popular" + query))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private boolean contains(JsonNode films, int filmId) {
        for (JsonNode film : films) {
            if (film.get("id").asInt() == filmId) {
                return true;
            }
        }
        return false;
    }

    private long occurrences(JsonNode films, int filmId) {
        long count = 0;
        for (JsonNode film : films) {
            if (film.get("id").asInt() == filmId) {
                count++;
            }
        }
        return count;
    }

    @Test
    void filtersByGenre() throws Exception {
        int actionFilm = createFilm("Genre action " + System.nanoTime(), "2000-01-01",
                ",\"genres\":[{\"id\":1}]");
        int comedyFilm = createFilm("Genre comedy " + System.nanoTime(), "2000-01-01",
                ",\"genres\":[{\"id\":2}]");

        JsonNode films = getPopular("?count=" + BIG_COUNT + "&genreId=1");

        assertThat(contains(films, actionFilm)).isTrue();
        assertThat(contains(films, comedyFilm)).isFalse();
    }

    @Test
    void filtersByYear() throws Exception {
        int oldFilm = createFilm("Year 1999 " + System.nanoTime(), "1999-05-05", "");
        int newFilm = createFilm("Year 2005 " + System.nanoTime(), "2005-05-05", "");

        JsonNode films = getPopular("?count=" + BIG_COUNT + "&year=1999");

        assertThat(contains(films, oldFilm)).isTrue();
        assertThat(contains(films, newFilm)).isFalse();
    }

    @Test
    void filtersByGenreAndYearTogether() throws Exception {
        int target = createFilm("Both filters " + System.nanoTime(), "1988-03-03",
                ",\"genres\":[{\"id\":3}]");
        int wrongYear = createFilm("Wrong year " + System.nanoTime(), "1999-03-03",
                ",\"genres\":[{\"id\":3}]");
        int wrongGenre = createFilm("Wrong genre " + System.nanoTime(), "1988-03-03",
                ",\"genres\":[{\"id\":4}]");

        JsonNode films = getPopular("?count=" + BIG_COUNT + "&genreId=3&year=1988");

        assertThat(contains(films, target)).isTrue();
        assertThat(contains(films, wrongYear)).isFalse();
        assertThat(contains(films, wrongGenre)).isFalse();
    }

    @Test
    void withoutFiltersReturnsFilmsOfDifferentGenresAndYears() throws Exception {
        int first = createFilm("No filters a " + System.nanoTime(), "1970-01-01",
                ",\"genres\":[{\"id\":1}]");
        int second = createFilm("No filters b " + System.nanoTime(), "2015-01-01", "");

        JsonNode films = getPopular("?count=" + BIG_COUNT);

        assertThat(contains(films, first)).isTrue();
        assertThat(contains(films, second)).isTrue();
    }

    @Test
    void filmWithTwoGenresIsNotDuplicated() throws Exception {
        int twoGenres = createFilm("Two genres " + System.nanoTime(), "2001-01-01",
                ",\"genres\":[{\"id\":1},{\"id\":2}]");

        JsonNode films = getPopular("?count=" + BIG_COUNT + "&genreId=1");

        assertThat(occurrences(films, twoGenres)).isEqualTo(1);
    }

    @Test
    void respectsCount() throws Exception {
        createFilm("Count a " + System.nanoTime(), "2002-01-01", "");
        createFilm("Count b " + System.nanoTime(), "2002-01-01", "");
        createFilm("Count c " + System.nanoTime(), "2002-01-01", "");

        JsonNode films = getPopular("?count=2");

        assertThat(films.size()).isEqualTo(2);
    }
}

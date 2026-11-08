package ru.yandex.practicum.filmorate.storage.genre;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Genre;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<Genre> getAll() {
        return jdbcTemplate.query("SELECT id, name FROM genres ORDER BY id", this::mapRow);
    }

    public Genre getById(int id) {
        return jdbcTemplate.query("SELECT id, name FROM genres WHERE id = ?", this::mapRow, id)
                .stream().findFirst().orElseThrow(() ->
                        new NotFoundException("Жанр с id=" + id + " не найден"));
    }

    private Genre mapRow(ResultSet rs, int rowNum) throws SQLException {
        Genre genre = new Genre();
        genre.setId(rs.getInt("id"));
        genre.setName(rs.getString("name"));
        return genre;
    }
}

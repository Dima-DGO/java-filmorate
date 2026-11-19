package ru.yandex.practicum.filmorate.storage.director;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
@Slf4j
@RequiredArgsConstructor
public class DirectorDbStorage implements DirectorStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<Director> getAll() {
        return jdbcTemplate.query("SELECT id, name FROM directors ORDER BY id", this::mapRow);
    }

    @Override
    public Director getById(int id) {
        return jdbcTemplate.query("SELECT id, name FROM directors WHERE id = ?", this::mapRow, id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public boolean existsById(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM directors WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    @Override
    public Director addDirector(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);
        director.setId(keyHolder.getKey().intValue());
        log.info("Добавлен режиссёр: id={} name={}", director.getId(), director.getName());
        return director;
    }

    @Override
    public Director updateDirector(Director director) {
        int rows = jdbcTemplate.update("UPDATE directors SET name = ? WHERE id = ?",
                director.getName(), director.getId());
        if (rows == 0) {
            throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден");
        }
        log.info("Обновлён режиссёр: id={} name={}", director.getId(), director.getName());
        return director;
    }

    @Override
    public void deleteDirector(int id) {
        jdbcTemplate.update("DELETE FROM directors WHERE id = ?", id);
        log.info("Удалён режиссёр: id={}", id);
    }

    @Override
    public List<Director> getByNameContaining(String query) {
        return jdbcTemplate.query(
                "SELECT id, name FROM directors WHERE LOWER(name) LIKE LOWER(?) ORDER BY id",
                this::mapRow, "%" + query + "%");
    }

    private Director mapRow(ResultSet rs, int rowNum) throws SQLException {
        Director director = new Director();
        director.setId(rs.getInt("id"));
        director.setName(rs.getString("name"));
        return director;
    }
}

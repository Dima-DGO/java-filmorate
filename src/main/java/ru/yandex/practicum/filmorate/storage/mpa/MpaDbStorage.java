package ru.yandex.practicum.filmorate.storage.mpa;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;

    public List<MpaRating> getAll() {
        return jdbcTemplate.query("SELECT id, name FROM mpa ORDER BY id", this::mapRow);
    }

    public MpaRating getById(int id) {
        return jdbcTemplate.query("SELECT id, name FROM mpa WHERE id = ?", this::mapRow, id)
                .stream().findFirst().orElseThrow(() ->
                        new NotFoundException("Рейтинг MPA с id=" + id + " не найден"));
    }

    private MpaRating mapRow(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpa = new MpaRating();
        mpa.setId(rs.getInt("id"));
        mpa.setName(rs.getString("name"));
        return mpa;
    }
}

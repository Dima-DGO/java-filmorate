package ru.yandex.practicum.filmorate.storage.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
@Slf4j
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void add(Event event) {
        String sql = """
                INSERT INTO events (user_id, entity_id, event_type, operation, created_at)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setInt(1, event.getUserId());
            ps.setInt(2, event.getEntityId());
            ps.setString(3, event.getEventType().name());
            ps.setString(4, event.getOperation().name());
            ps.setLong(5, event.getTimestamp());
            return ps;
        }, keyHolder);
        if (keyHolder.getKey() != null) {
            event.setEventId(keyHolder.getKey().intValue());
        }
        log.info("Записано событие: userId={} type={} operation={} entityId={}",
                event.getUserId(), event.getEventType(), event.getOperation(), event.getEntityId());
    }

    @Override
    public List<Event> getFeed(int userId) {
        return jdbcTemplate.query("""
                SELECT event_id, user_id, entity_id, event_type, operation, created_at
                FROM events
                WHERE user_id = ?
                ORDER BY created_at, event_id
                """, this::mapRow, userId);
    }

    private Event mapRow(ResultSet rs, int rowNum) throws SQLException {
        Event event = new Event();
        event.setEventId(rs.getInt("event_id"));
        event.setUserId(rs.getInt("user_id"));
        event.setEntityId(rs.getInt("entity_id"));
        event.setEventType(EventType.valueOf(rs.getString("event_type")));
        event.setOperation(Operation.valueOf(rs.getString("operation")));
        event.setTimestamp(rs.getLong("created_at"));
        return event;
    }
}

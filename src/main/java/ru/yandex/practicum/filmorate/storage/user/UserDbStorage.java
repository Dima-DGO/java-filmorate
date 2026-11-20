package ru.yandex.practicum.filmorate.storage.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Repository("userDbStorage")
@Slf4j
@RequiredArgsConstructor
public class UserDbStorage implements UserStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public User addUser(User user) {
        String sql = "INSERT INTO users (email, login, name, birthday) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, user.getEmail());
            ps.setString(2, user.getLogin());
            ps.setString(3, user.getName());
            ps.setDate(4, Date.valueOf(user.getBirthday()));
            return ps;
        }, keyHolder);

        user.setId(keyHolder.getKey().intValue());
        log.info("Добавлен пользователь: id={} login={}", user.getId(), user.getLogin());
        return user;
    }

    @Override
    public User updateUser(User user) {
        String sql = "UPDATE users SET email = ?, login = ?, name = ?, birthday = ? WHERE user_id = ?";
        int rows = jdbcTemplate.update(sql,
                user.getEmail(), user.getLogin(), user.getName(),
                Date.valueOf(user.getBirthday()), user.getId());

        if (rows == 0) {
            throw new NotFoundException("Пользователь с id=" + user.getId() + " не найден");
        }
        log.info("Обновлён пользователь: id={} login={}", user.getId(), user.getLogin());
        return user;
    }

    @Override
    public User getUserById(int id) {
        return jdbcTemplate.query(
                "SELECT user_id, email, login, name, birthday FROM users WHERE user_id = ?",
                this::mapRowToUser, id
        ).stream().findFirst().orElse(null);
    }

    @Override
    public List<User> getAllUsers() {
        return jdbcTemplate.query(
                "SELECT user_id, email, login, name, birthday FROM users ORDER BY user_id",
                this::mapRowToUser
        );
    }

    @Override
    public void deleteUser(int id) {
        jdbcTemplate.update("DELETE FROM users WHERE user_id = ?", id);
    }


    private User mapRowToUser(ResultSet rs, int rowNum) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("user_id"));
        user.setEmail(rs.getString("email"));
        user.setLogin(rs.getString("login"));
        user.setName(rs.getString("name"));
        user.setBirthday(rs.getDate("birthday").toLocalDate());
        return user;
    }


    @Override
    public void addFriend(int userId, int friendId) {
        jdbcTemplate.update("MERGE INTO friendships (user_id, friend_id) KEY (user_id, friend_id) VALUES (?, ?)",
                userId, friendId);
    }

    @Override
    public void removeFriend(int userId, int friendId) {
        jdbcTemplate.update("DELETE FROM friendships WHERE user_id = ? AND friend_id = ?", userId, friendId);
    }

    @Override
    public Set<Integer> getFriendIds(int userId) {
        return jdbcTemplate.query(
                "SELECT friend_id FROM friendships WHERE user_id = ?",
                rs -> {
                    Set<Integer> ids = new HashSet<>();
                    while (rs.next()) {
                        ids.add(rs.getInt("friend_id"));
                    }
                    return ids;
                },
                userId);
    }

    @Override
    public List<User> getFriends(int userId) {
        return jdbcTemplate.query("""
                SELECT u.user_id, u.email, u.login, u.name, u.birthday
                FROM friendships f
                JOIN users u ON u.user_id = f.friend_id
                WHERE f.user_id = ?
                ORDER BY u.user_id
                """, this::mapRowToUser, userId);
    }

    @Override
    public List<User> getCommonFriends(int userId, int otherUserId) {
        return jdbcTemplate.query("""
                SELECT u.user_id, u.email, u.login, u.name, u.birthday
                FROM friendships f1
                JOIN friendships f2 ON f1.friend_id = f2.friend_id
                JOIN users u ON u.user_id = f1.friend_id
                WHERE f1.user_id = ? AND f2.user_id = ?
                ORDER BY u.user_id
                """, this::mapRowToUser, userId, otherUserId);
    }

    @Override
    public List<Integer> getSimilarUserIds(int userId) {
        return jdbcTemplate.query("""
                WITH target_likes AS (
                    SELECT film_id FROM likes WHERE user_id = ?
                ),
                scores AS (
                    SELECT l.user_id, COUNT(DISTINCT l.film_id) AS common_likes
                    FROM likes l
                    JOIN target_likes t ON t.film_id = l.film_id
                    WHERE l.user_id <> ?
                    GROUP BY l.user_id
                )
                SELECT user_id
                FROM scores
                WHERE common_likes = (SELECT MAX(common_likes) FROM scores)
                ORDER BY user_id
                """,
                (rs, rowNum) -> rs.getInt("user_id"),
                userId, userId);
    }
}

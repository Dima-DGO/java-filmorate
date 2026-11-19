package ru.yandex.practicum.filmorate.storage.film;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Repository("filmDbStorage")
@Slf4j
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public Film addFilm(Film film) {
        String sql = """
                INSERT INTO films (name, description, release_date, duration, mpa_id)
                VALUES (?, ?, ?, ?, ?)
                """;
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (film.getMpa() != null) {
                ps.setInt(5, film.getMpa().getId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);

        film.setId(keyHolder.getKey().intValue());
        saveGenres(film.getId(), film.getGenres());
        saveDirectors(film.getId(), film.getDirectors());
        log.info("Добавлен фильм: id={} name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    @Transactional
    public Film updateFilm(Film film) {
        String sql = """
                UPDATE films SET name = ?, description = ?, release_date = ?, duration = ?, mpa_id = ?
                WHERE film_id = ?
                """;
        int rows = jdbcTemplate.update(sql,
                film.getName(), film.getDescription(),
                Date.valueOf(film.getReleaseDate()), film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId());

        if (rows == 0) {
            throw new NotFoundException("Фильм с id=" + film.getId() + " не найден");
        }

        jdbcTemplate.update("DELETE FROM film_genres WHERE film_id = ?", film.getId());
        jdbcTemplate.update("DELETE FROM film_directors WHERE film_id = ?", film.getId());
        saveGenres(film.getId(), film.getGenres());
        saveDirectors(film.getId(), film.getDirectors());

        log.info("Обновлён фильм: id={} name={}", film.getId(), film.getName());
        return film;
    }

    @Override
    public Film getFilmById(int id) {
        Film film = jdbcTemplate.query("""
                SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, m.name AS mpa_name
                FROM films f
                LEFT JOIN mpa m ON m.id = f.mpa_id
                WHERE f.film_id = ?
                """, this::mapRowToFilm, id
        ).stream().findFirst().orElse(null);

        if (film != null) {
            loadGenres(List.of(film));
            loadDirectors(List.of(film));
        }
        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, m.name AS mpa_name
                FROM films f
                LEFT JOIN mpa m ON m.id = f.mpa_id
                ORDER BY f.film_id
                """, this::mapRowToFilm);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public List<Film> getPopularFilms(int count) {
        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, m.name AS mpa_name,
                       COUNT(l.user_id) AS likes_count
                FROM films f
                LEFT JOIN mpa m ON m.id = f.mpa_id
                LEFT JOIN likes l ON f.film_id = l.film_id
                GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name
                ORDER BY likes_count DESC, f.film_id
                LIMIT ?
                """, this::mapRowToFilm, count);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public List<Film> getCommonFilms(int userId, int friendId) {
        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, m.name AS mpa_name,
                       COUNT(l.user_id) AS likes_count
                FROM films f
                JOIN likes ul ON ul.film_id = f.film_id AND ul.user_id = ?
                JOIN likes fl ON fl.film_id = f.film_id AND fl.user_id = ?
                LEFT JOIN mpa m ON m.id = f.mpa_id
                LEFT JOIN likes l ON l.film_id = f.film_id
                GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, f.mpa_id, m.name
                ORDER BY likes_count DESC, f.film_id
                """, this::mapRowToFilm, userId, friendId);
        loadGenres(films);
        loadDirectors(films);
        return films;
    }

    @Override
    public void deleteFilm(int id) {
        jdbcTemplate.update("DELETE FROM films WHERE film_id = ?", id);
    }

    private void saveGenres(int filmId, Set<Genre> genres) {
        if (genres == null || genres.isEmpty()) {
            return;
        }
        List<Object[]> params = genres.stream()
                .map(g -> new Object[]{filmId, g.getId()})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate("""
                MERGE INTO film_genres (film_id, genre_id) KEY (film_id, genre_id) VALUES (?, ?)
                """, params);
    }

    private void loadGenres(List<Film> films) {
        List<Integer> ids = films.stream().map(Film::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        String inClause = String.join(",", Collections.nCopies(ids.size(), "?"));

        Map<Integer, Set<Genre>> genresByFilm = jdbcTemplate.query("""
                SELECT fg.film_id, g.id, g.name
                FROM film_genres fg
                JOIN genres g ON g.id = fg.genre_id
                WHERE fg.film_id IN (%s)
                ORDER BY fg.film_id, g.id
                """.formatted(inClause), rs -> {
            Map<Integer, Set<Genre>> result = new HashMap<>();
            while (rs.next()) {
                Genre genre = new Genre();
                genre.setId(rs.getInt("id"));
                genre.setName(rs.getString("name"));
                result.computeIfAbsent(rs.getInt("film_id"), k -> new LinkedHashSet<>()).add(genre);
            }
            return result;
        }, ids.toArray());

        for (Film film : films) {
            film.getGenres().clear();
            film.getGenres().addAll(genresByFilm.getOrDefault(film.getId(), new HashSet<>()));
        }
    }

    private void saveDirectors(int filmId, Set<Director> directors) {
        if (directors == null || directors.isEmpty()) {
            return;
        }
        List<Object[]> params = directors.stream()
                .map(director -> {
                    if (director.getId() == null || !existsDirector(director.getId())) {
                        throw new NotFoundException("Режиссёр с id=" + director.getId() + " не найден");
                    }
                    return new Object[]{filmId, director.getId()};
                })
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate("""
                MERGE INTO film_directors (film_id, director_id) KEY (film_id, director_id) VALUES (?, ?)
                """, params);
    }

    private boolean existsDirector(int id) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM directors WHERE id = ?", Integer.class, id);
        return count != null && count > 0;
    }

    private void loadDirectors(List<Film> films) {
        List<Integer> ids = films.stream().map(Film::getId).collect(Collectors.toList());
        if (ids.isEmpty()) {
            return;
        }
        String inClause = String.join(",", Collections.nCopies(ids.size(), "?"));
        Map<Integer, Set<Director>> directorsByFilm = jdbcTemplate.query("""
                SELECT fd.film_id, d.id, d.name
                FROM film_directors fd
                JOIN directors d ON d.id = fd.director_id
                WHERE fd.film_id IN (%s)
                ORDER BY fd.film_id, d.id
                """.formatted(inClause), rs -> {
            Map<Integer, Set<Director>> result = new HashMap<>();
            while (rs.next()) {
                Director director = new Director();
                director.setId(rs.getInt("id"));
                director.setName(rs.getString("name"));
                result.computeIfAbsent(rs.getInt("film_id"), k -> new LinkedHashSet<>()).add(director);
            }
            return result;
        }, ids.toArray());

        for (Film film : films) {
            film.getDirectors().clear();
            film.getDirectors().addAll(directorsByFilm.getOrDefault(film.getId(), new LinkedHashSet<>()));
        }
    }

    private Film mapRowToFilm(ResultSet rs, int rowNum) throws SQLException {
        MpaRating mpa = null;
        int mpaId = rs.getInt("mpa_id");
        if (!rs.wasNull()) {
            mpa = new MpaRating();
            mpa.setId(mpaId);
            mpa.setName(rs.getString("mpa_name"));
        }
        Film film = new Film();
        film.setId(rs.getInt("film_id"));
        film.setName(rs.getString("name"));
        film.setDescription(rs.getString("description"));
        film.setReleaseDate(rs.getDate("release_date").toLocalDate());
        film.setDuration(rs.getInt("duration"));
        film.setMpa(mpa);
        return film;
    }

    @Override
    public void addLike(int filmId, int userId) {
        jdbcTemplate.update("MERGE INTO likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)",
                filmId, userId);
    }

    @Override
    public void removeLike(int filmId, int userId) {
        int rows = jdbcTemplate.update("DELETE FROM likes WHERE film_id = ? AND user_id = ?", filmId, userId);
        if (rows == 0) {
            throw new NotFoundException("Лайк пользователя id=" + userId + " у фильма id=" + filmId + " не найден");
        }
    }

    @Override
    public List<Film> getFilmsByDirector(int directorId, String sortBy) {
        String orderBy = "year".equals(sortBy)
                ? "f.release_date, f.film_id"
                : "likes_count DESC, f.film_id";

        List<Film> films = jdbcTemplate.query("""
                SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_id, m.name AS mpa_name, COUNT(l.user_id) AS likes_count
                FROM films f
                LEFT JOIN mpa m ON m.id = f.mpa_id
                JOIN film_directors fd ON fd.film_id = f.film_id
                LEFT JOIN likes l ON l.film_id = f.film_id
                WHERE fd.director_id = ?
                GROUP BY f.film_id, f.name, f.description, f.release_date,
                         f.duration, f.mpa_id, m.name
                ORDER BY %s
                """.formatted(orderBy), this::mapRowToFilm, directorId);

        loadGenres(films);
        loadDirectors(films);
        return films;
    }
    public Set<Integer> getLikedFilmIds(int userId) {
        return new HashSet<>(jdbcTemplate.queryForList("SELECT film_id FROM likes WHERE user_id = ?",
                Integer.class, userId));
    }

    @Override
    public List<Film> getFilmsLikedByUsersExcluding(Collection<Integer> userIds, int excludeUserId) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }

        String inClause = String.join(",", Collections.nCopies(userIds.size(), "?"));

        String sql = """
                SELECT DISTINCT f.film_id, f.name, f.description, f.release_date, f.duration,
                f.mpa_id, m.name AS mpa_name
                FROM films f
                JOIN likes AS l ON l.film_id = f.film_id
                LEFT JOIN mpa AS m ON m.id = f.mpa_id
                WHERE l.user_id IN (%s)
                  AND NOT EXISTS (
                      SELECT 1 FROM likes l2
                      WHERE l2.user_id = ? AND l2.film_id = f.film_id
                  )
                ORDER BY f.film_id
                """.formatted(inClause);

        Object[] args = new Object[userIds.size() + 1];
        int i = 0;
        for (Integer uId : userIds) {
            args[i] = uId;
            i++;
        }
        args[i] = excludeUserId;

        List<Film> films = jdbcTemplate.query(sql, this::mapRowToFilm, args);
        loadGenres(films);
        loadDirectors(films);

        return films;
    }
}


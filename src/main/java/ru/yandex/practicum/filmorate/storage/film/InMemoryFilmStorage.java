package ru.yandex.practicum.filmorate.storage.film;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;

@Component
@Slf4j
public class InMemoryFilmStorage implements FilmStorage {

    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;

    @Override
    public Film addFilm(Film film) {
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}", film.getName());
        return film;
    }

    @Override
    public Film updateFilm(Film film) {
        if (film.getId() == null) {
            throw new IllegalArgumentException("ID фильма не может быть null при обновлении");
        }

        Film existingFilm = films.get(film.getId());
        if (existingFilm == null) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }

        existingFilm.setName(film.getName());
        existingFilm.setDescription(film.getDescription());
        existingFilm.setReleaseDate(film.getReleaseDate());
        existingFilm.setDuration(film.getDuration());

        log.info("Обновлён фильм: {}", existingFilm.getName());
        return existingFilm;
    }

    @Override
    public Film getFilmById(int id) {
        Film film = films.get(id);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        return film;
    }

    @Override
    public List<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов. Всего фильмов: {}", films.size());
        return new ArrayList<>(films.values());
    }

    @Override
    public void deleteFilm(int id) {
        if (!films.containsKey(id)) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        films.remove(id);
        log.info("Удален фильм с ID: {}", id);
    }
}


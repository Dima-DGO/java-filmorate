package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/films")
public class FilmController {

    private final Map<Integer, Film> films = new HashMap<>();
    private int nextId = 1;
    private final ValidationService validationService;

    public FilmController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping
    public Film addFilm(@RequestBody Film film) {
        validationService.validateFilm(film);
        film.setId(nextId++);
        films.put(film.getId(), film);
        log.info("Добавлен фильм: {}", film.getName());
        return film;
    }

    @PutMapping
    public Film updateFilm(@RequestBody Film film) {
        validationService.validateFilm(film);

        if (film.getId() == null) {
            throw new IllegalArgumentException("ID фильма не может быть null при обновлении");
        }

        Film existingFilm = films.get(film.getId());
        if (existingFilm == null) {
            throw new IllegalArgumentException("Фильм с ID " + film.getId() + " не найден");
        }

        existingFilm.setName(film.getName());
        existingFilm.setDescription(film.getDescription());
        existingFilm.setReleaseDate(film.getReleaseDate());
        existingFilm.setDuration(film.getDuration());

        log.info("Обновлён фильм: {}", existingFilm.getName());
        return existingFilm;
    }

    @GetMapping
    public List<Film> getAllFilms() {
        log.info("Получен запрос на получение всех фильмов. Всего фильмов: {}", films.size());
        return new ArrayList<>(films.values());
    }
}


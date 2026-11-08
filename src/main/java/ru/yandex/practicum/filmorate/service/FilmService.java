package ru.yandex.practicum.filmorate.service;


import java.util.Set;
import java.util.LinkedHashSet;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.genre.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.mpa.MpaDbStorage;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FilmService {

    private final FilmStorage filmStorage;
    private final UserService userService;
    private final ValidationService validationService;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;

    public Film addFilm(Film film) {
        validationService.validateFilm(film);
        validateFilmRefs(film);
        filmStorage.addFilm(film);
        return filmStorage.getFilmById(film.getId());
    }

    public Film updateFilm(Film film) {
        if (film.getId() == null) {
            throw new ValidationException("ID фильма не может быть null при обновлении");
        }
        getFilmById(film.getId());
        validationService.validateFilm(film);
        validateFilmRefs(film);
        filmStorage.updateFilm(film);
        return filmStorage.getFilmById(film.getId());
    }

    public List<Film> getAllFilms() {
        return filmStorage.getAllFilms();
    }

    public Film getFilmById(int id) {
        Film film = filmStorage.getFilmById(id);
        if (film == null) {
            throw new NotFoundException("Фильм с ID " + id + " не найден");
        }
        return film;
    }

    public void deleteFilm(int id) {
        getFilmById(id);
        filmStorage.deleteFilm(id);
    }

    public void addLike(int filmId, int userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(int filmId, int userId) {
        getFilmById(filmId);
        userService.getUserById(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopularFilms(int count) {
        return filmStorage.getAllFilms().stream()
                .sorted(Comparator.comparingInt((Film f) -> f.getLikes().size()).reversed())
                .limit(count)
                .collect(Collectors.toList());
    }


    private void validateFilmRefs(Film film) {
        if (film.getMpa() != null) {
            MpaRating mpa = mpaStorage.getById(film.getMpa().getId());
            if (mpa == null) {
                throw new NotFoundException("Рейтинг MPA с id=" + film.getMpa().getId() + " не найден");
            }
            film.setMpa(mpa);
        }
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            Set<Genre> validated = film.getGenres().stream()
                    .map(genre -> {
                        Genre stored = genreStorage.getById(genre.getId());
                        if (stored == null) {
                            throw new NotFoundException("Жанр с id=" + genre.getId() + " не найден");
                        }
                        return stored;
                    })
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            film.setGenres(validated);
        }
    }

}
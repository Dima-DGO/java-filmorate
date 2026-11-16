package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;

public interface FilmStorage {
    Film addFilm(Film film);

    Film updateFilm(Film film);

    Film getFilmById(int id);

    List<Film> getAllFilms();

    void deleteFilm(int id);


    void addLike(int filmId, int userId);

    void removeLike(int filmId, int userId);

    List<Film> getPopularFilms(int count);

    List<Film> getPopularFilms(int count, Integer genreId, Integer year);

    List<Film> getFilmsByDirector(int directorId, String sortBy);

    List<Film> searchFilms(String query, List<String> by);

    List<Film> getCommonFilms(int userId, int friendId);

}

package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;
import java.util.List;
import java.util.Collection;
import java.util.Set;

public interface FilmStorage {
    Film addFilm(Film film);

    Film updateFilm(Film film);

    Film getFilmById(int id);

    List<Film> getAllFilms();

    List<Film> searchFilms(String query, List<String> searchFields);

    void deleteFilm(int id);

    void addLike(int filmId, int userId);

    void removeLike(int filmId, int userId);

    List<Film> getPopularFilms(int count);

    Set<Integer> getLikedFilmIds(int userId);

    List<Film> getFilmsLikedByUsersExcluding(Collection<Integer> userIds, int excludeUserId);

    List<Film> getFilmsByDirector(int directorId, String sortBy);

    List<Film> getCommonFilms(int userId, int friendId);

}

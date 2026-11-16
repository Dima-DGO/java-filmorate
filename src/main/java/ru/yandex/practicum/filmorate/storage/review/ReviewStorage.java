package ru.yandex.practicum.filmorate.storage.review;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;

public interface ReviewStorage {
    Review add(Review review);

    Review update(Review review);

    void delete(int reviewId);

    Review getById(int reviewId);

    List<Review> getByFilmId(int filmId, String sortBy);

    void addMark(int reviewId, int userId, boolean isPositive);

    void removeMark(int reviewId, int userId);
}

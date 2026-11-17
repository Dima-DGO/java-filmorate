package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;

    public Review addReview(Review review) {
        validateContent(review);
        if (review.getUserId() == null || review.getFilmId() == null) {
            throw new ValidationException("Нужно указать автора и фильм");
        }
        userService.getUserById(review.getUserId());
        filmService.getFilmById(review.getFilmId());
        return reviewStorage.addReview(review);
    }

    public Review updateReview(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("Нужно указать ID отзыва");
        }
        getReviewById(review.getReviewId());
        validateContent(review);
        return reviewStorage.updateReview(review);
    }

    public Review getReviewById(int id) {
        Review review = reviewStorage.getReviewById(id);
        if (review == null) {
            throw new NotFoundException("Отзыв с ID " + id + " не найден");
        }
        return review;
    }

    public List<Review> getAllReview(Integer filmId, int count) {
        if (count < 0) {
            throw new ValidationException("Количество отзывов не может быть отрицательным");
        }
        return reviewStorage.getAllReview(filmId, count);
    }

    public void addReaction(int reviewId, int userId, boolean positive) {
        validateReaction(reviewId, userId);
        reviewStorage.addReaction(reviewId, userId, positive);
    }

    public void removeReaction(int reviewId, int userId, boolean positive) {
        validateReaction(reviewId, userId);
        reviewStorage.removeReaction(reviewId, userId, positive);
    }

    public void deleteReview(int id) {
        getReviewById(id);
        reviewStorage.deleteReview(id);
    }

    private void validateReaction(int reviewId, int userId) {
        getReviewById(reviewId);
        userService.getUserById(userId);
    }

    private void validateContent(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Текст отзыва не может быть пустым");
        }
        if (review.getPositive() == null) {
            throw new ValidationException("Нужно указать, положительный ли отзыв");
        }
    }
}

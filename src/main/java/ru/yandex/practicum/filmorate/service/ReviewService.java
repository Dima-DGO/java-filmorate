package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.review.ReviewStorage;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewService {
    private final ReviewStorage reviewStorage;
    private final UserService userService;
    private final FilmService filmService;
    private final EventService eventService;

    @Transactional
    public Review addReview(Review review) {
        validateContent(review);
        if (review.getUserId() == null || review.getFilmId() == null) {
            throw new ValidationException("Нужно указать автора и фильм");
        }
        userService.getUserById(review.getUserId());
        filmService.getFilmById(review.getFilmId());
        Review saved = reviewStorage.addReview(review);
        eventService.addEvent(saved.getUserId(), EventType.REVIEW, Operation.ADD, saved.getReviewId());
        return saved;
    }

    @Transactional
    public Review updateReview(Review review) {
        if (review.getReviewId() == null) {
            throw new ValidationException("Нужно указать ID отзыва");
        }
        Review stored = getReviewById(review.getReviewId());
        validateContent(review);
        Review updated = reviewStorage.updateReview(review);
        eventService.addEvent(stored.getUserId(), EventType.REVIEW, Operation.UPDATE, stored.getReviewId());
        return updated;
    }

    public Review getReviewById(int id) {
        Review review = reviewStorage.getReviewById(id);
        if (review == null) {
            throw new NotFoundException("Отзыв с ID " + id + " не найден");
        }
        return review;
    }

    public List<Review> getAllReview(Integer filmId, int count) {
        if (count < 1) {
            throw new ValidationException("Количество отзывов должно быть положительным");
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

    @Transactional
    public void deleteReview(int id) {
        Review stored = getReviewById(id);
        reviewStorage.deleteReview(id);
        eventService.addEvent(stored.getUserId(), EventType.REVIEW, Operation.REMOVE, id);
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

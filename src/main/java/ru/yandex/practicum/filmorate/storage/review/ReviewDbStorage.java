package ru.yandex.practicum.filmorate.storage.review;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {
    private static final String SELECT_REVIEWS = """
            SELECT r.*,
                   COALESCE((SELECT SUM(CASE WHEN rr.is_positive THEN 1 ELSE -1 END)
                             FROM review_reactions rr WHERE rr.review_id = r.review_id), 0) AS useful
            FROM reviews r
            """;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public Review addReview(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, review.getContent());
            statement.setBoolean(2, review.getPositive());
            statement.setInt(3, review.getUserId());
            statement.setInt(4, review.getFilmId());
            return statement;
        }, keyHolder);
        return getReviewById(keyHolder.getKey().intValue());
    }

    @Override
    public Review updateReview(Review review) {
        jdbcTemplate.update("UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?",
                review.getContent(), review.getPositive(), review.getReviewId());
        return getReviewById(review.getReviewId());
    }

    @Override
    public Review getReviewById(int id) {
        return jdbcTemplate.query(SELECT_REVIEWS + " WHERE r.review_id = ?", this::mapRowToReview, id)
                .stream().findFirst().orElse(null);
    }

    @Override
    public List<Review> getAllReview(Integer filmId, int count) {
        String order = " ORDER BY useful DESC, r.review_id LIMIT ?";
        if (filmId == null) {
            return jdbcTemplate.query(SELECT_REVIEWS + order, this::mapRowToReview, count);
        }
        return jdbcTemplate.query(SELECT_REVIEWS + " WHERE r.film_id = ?" + order,
                this::mapRowToReview, filmId, count);
    }

    @Override
    public void addReaction(int reviewId, int userId, boolean positive) {
        jdbcTemplate.update("""
                MERGE INTO review_reactions (review_id, user_id, is_positive)
                KEY (review_id, user_id) VALUES (?, ?, ?)
                """, reviewId, userId, positive);
    }

    @Override
    public void removeReaction(int reviewId, int userId, boolean positive) {
        jdbcTemplate.update("""
                DELETE FROM review_reactions WHERE review_id = ? AND user_id = ? AND is_positive = ?
                """, reviewId, userId, positive);
    }

    @Override
    public void deleteReview(int id) {
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id = ?", id);
    }

    private Review mapRowToReview(ResultSet rs, int rowNum) throws SQLException {
        Review review = new Review();
        review.setReviewId(rs.getInt("review_id"));
        review.setContent(rs.getString("content"));
        review.setPositive(rs.getBoolean("is_positive"));
        review.setUserId(rs.getInt("user_id"));
        review.setFilmId(rs.getInt("film_id"));
        review.setUseful(rs.getInt("useful"));
        return review;
    }
}

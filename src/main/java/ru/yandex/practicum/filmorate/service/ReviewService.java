package ru.yandex.practicum.filmorate.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Review;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    public List<Review> getAllReview() {
        return new ArrayList<>();
    }
}

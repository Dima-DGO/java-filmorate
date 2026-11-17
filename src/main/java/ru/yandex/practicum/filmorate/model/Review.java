package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Review {
    private int id;
    private String description;
    private ReviewType type;
    private ReviewEvaluation evaluation;
    private int rating = 0;
}

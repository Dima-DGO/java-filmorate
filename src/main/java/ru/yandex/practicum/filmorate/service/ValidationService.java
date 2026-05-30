package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.User;

import org.springframework.stereotype.Service;
import java.time.LocalDate;

@Service
public class ValidationService {

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    public void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }

        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }

        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(LocalDate.of(1895, 12, 28))) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }

        if (film.getDuration() != null && film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }

    public void validateUser(User user) {

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ValidationException("Электронная почта не может быть пустой");
        }
        if (!user.getEmail().contains("@")) {
            throw new ValidationException("Электронная почта должна содержать символ @");
        }

        if (user.getLogin() == null || user.getLogin().isBlank()) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getLogin().contains(" ")) {
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }

        if (user.getBirthday() != null && user.getBirthday().isAfter(LocalDate.now())) {
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
    }

    public static LocalDate getMinReleaseDate() {
        return MIN_RELEASE_DATE;
    }

}


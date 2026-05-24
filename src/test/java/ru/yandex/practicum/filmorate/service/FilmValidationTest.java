package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class FilmValidationTest {

    @Autowired
    private ValidationService validationService;
    private static final LocalDate FIXED_DATE = LocalDate.of(2023, 1, 1);

    @Test
    void testEmptyName() {
        Film film = new Film();
        film.setName("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateFilm(film)
        );
        assertEquals("Название фильма не может быть пустым", exception.getMessage());
    }

    @Test
    void testNullName() {
        Film film = new Film();
        film.setName(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateFilm(film)
        );
        assertEquals("Название фильма не может быть пустым", exception.getMessage());
    }

    @Test
    void testDescriptionTooLong() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("a".repeat(201));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateFilm(film)
        );
        assertEquals("Максимальная длина описания — 200 символов", exception.getMessage());
    }

    @Test
    void testDescriptionExactly200Chars() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDescription("a".repeat(200));
        assertDoesNotThrow(() -> validationService.validateFilm(film));
    }

    @Test
    void testReleaseDateBeforeMin() {
        Film film = new Film();
        film.setName("Test Film");
        film.setReleaseDate(LocalDate.of(1895, 12, 27));
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateFilm(film)
        );
        assertEquals("Дата релиза не может быть раньше 28 декабря 1895 года", exception.getMessage());
    }

    @Test
    void testReleaseDateExactlyMin() {
        Film film = new Film();
        film.setName("Test Film");
        film.setReleaseDate(LocalDate.of(1895, 12, 28));
        assertDoesNotThrow(() -> validationService.validateFilm(film));
    }

    @Test
    void testValidReleaseDate() {
        Film film = new Film();
        film.setName("Test Film");
        film.setReleaseDate(FIXED_DATE);
        assertDoesNotThrow(() -> validationService.validateFilm(film));
    }

    @Test
    void testNegativeDuration() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDuration(-5);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateFilm(film)
        );
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void testZeroDuration() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDuration(0);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateFilm(film)
        );
        assertEquals("Продолжительность фильма должна быть положительным числом", exception.getMessage());
    }

    @Test
    void testPositiveDuration() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDuration(1);
        assertDoesNotThrow(() -> validationService.validateFilm(film));
    }

    @Test
    void testValidDuration() {
        Film film = new Film();
        film.setName("Test Film");
        film.setDuration(120);
        assertDoesNotThrow(() -> validationService.validateFilm(film));
    }

    @Test
    void testFullyValidFilm() {
        Film film = new Film();
        film.setName("Valid Film");
        film.setDescription("Valid description");
        film.setReleaseDate(FIXED_DATE);
        film.setDuration(90);
        assertDoesNotThrow(() -> validationService.validateFilm(film));
    }
}

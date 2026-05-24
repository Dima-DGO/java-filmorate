package ru.yandex.practicum.filmorate.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest
class UserValidationTest {

    @Autowired
    private ValidationService validationService;
    private static final LocalDate FIXED_DATE = LocalDate.of(2023, 1, 1);

    @Test
    void testEmptyEmail() {
        User user = new User();
        user.setEmail("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateUser(user)
        );
        assertEquals("Электронная почта не может быть пустой", exception.getMessage());
    }

    @Test
    void testNullEmail() {
        User user = new User();
        user.setEmail(null);
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateUser(user)
        );
        assertEquals("Электронная почта не может быть пустой", exception.getMessage());
    }

    @Test
    void testEmailWithoutAt() {
        User user = new User();
        user.setEmail("testemail.com");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateUser(user)
        );
        assertEquals("Электронная почта должна содержать символ @", exception.getMessage());
    }

    @Test
    void testEmptyLogin() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateUser(user)
        );
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }

    @Test
    void testLoginWithSpaces() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("test user");
        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateUser(user)
        );
        assertEquals("Логин не может быть пустым и содержать пробелы", exception.getMessage());
    }


    @Test
    void testFutureBirthday() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testuser");
        user.setBirthday(LocalDate.now().plusDays(1));

        ValidationException exception = assertThrows(ValidationException.class,
                () -> validationService.validateUser(user)
        );
        assertEquals("Дата рождения не может быть в будущем", exception.getMessage());
    }

    @Test
    void testPastBirthday() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testuser");
        user.setBirthday(FIXED_DATE.minusYears(10));
        assertDoesNotThrow(() -> validationService.validateUser(user));
    }

    @Test
    void testNullBirthday() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testuser");
        user.setBirthday(null);
        assertDoesNotThrow(() -> validationService.validateUser(user));
    }

    @Test
    void testNameIsNullUseLogin() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testlogin");
        user.setName(null);
        user.setBirthday(FIXED_DATE);
        assertDoesNotThrow(() -> validationService.validateUser(user));
        // В контроллере будет установлено имя = логин
    }

    @Test
    void testValidName() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(FIXED_DATE);
        assertDoesNotThrow(() -> validationService.validateUser(user));
    }

    @Test
    void testFullyValidUser() {
        User user = new User();
        user.setEmail("test@email.com");
        user.setLogin("testuser");
        user.setName("Test User");
        user.setBirthday(FIXED_DATE);
        assertDoesNotThrow(() -> validationService.validateUser(user));
    }
}

package ru.yandex.practicum.filmorate.controller;

import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.ValidationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Slf4j
@RequestMapping("/users")
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;
    private final ValidationService validationService;

    public UserController(ValidationService validationService) {
        this.validationService = validationService;
    }

    @PostMapping
    public User addUser(@RequestBody User user) {
        validationService.validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }

        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user.getLogin());
        return user;
    }

    @PutMapping
    public User updateUser(@RequestBody User user) {
        validationService.validateUser(user);

        if (user.getId() == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null при обновлении");
        }

        User existingUser = users.get(user.getId());
        if (existingUser == null) {
            throw new IllegalArgumentException("Пользователь с ID " + user.getId() + " не найден");
        }

        existingUser.setEmail(user.getEmail());
        existingUser.setLogin(user.getLogin());

        // Обработка имени: если пустое, устанавливаем равным логину
        if (user.getName() == null || user.getName().isBlank()) {
            existingUser.setName(user.getLogin());
        } else {
            existingUser.setName(user.getName());
        }

        existingUser.setBirthday(user.getBirthday());

        log.info("Обновлён пользователь: {}", existingUser.getLogin());
        return existingUser;
    }

    @GetMapping
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Всего пользователей: {}", users.size());
        return new ArrayList<>(users.values());
    }
}





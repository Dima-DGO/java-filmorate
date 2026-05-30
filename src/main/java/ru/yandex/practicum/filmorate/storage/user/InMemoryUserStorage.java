package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @Override
    public User addUser(User user) {
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null при обновлении");
        }

        User existingUser = users.get(user.getId());
        if (existingUser == null) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }

        existingUser.setEmail(user.getEmail());
        existingUser.setLogin(user.getLogin());

        if (user.getName() == null || user.getName().isBlank()) {
            existingUser.setName(user.getLogin());
        } else {
            existingUser.setName(user.getName());
        }

        existingUser.setBirthday(user.getBirthday());

        log.info("Обновлён пользователь: {}", existingUser.getLogin());
        return existingUser;
    }

    @Override
    public User getUserById(int id) {
        User user = users.get(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        return user;
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Всего пользователей: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public void deleteUser(int id) {
        if (!users.containsKey(id)) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        users.remove(id);
        log.info("Удален пользователь с ID: {}", id);
    }
}

package ru.yandex.practicum.filmorate.storage.user;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.User;

import java.util.*;

@Component
@Slf4j
public class InMemoryUserStorage implements UserStorage {

    private final Map<Integer, User> users = new HashMap<>();
    private int nextId = 1;

    @Override
    public User addUser(User user) {
        user.setId(nextId++);
        users.put(user.getId(), user);
        log.info("Добавлен пользователь: {}", user.getLogin());
        return user;
    }

    @Override
    public User updateUser(User user) {
        User existingUser = users.get(user.getId());
        existingUser.setEmail(user.getEmail());
        existingUser.setLogin(user.getLogin());
        existingUser.setName(user.getName());
        existingUser.setBirthday(user.getBirthday());

        log.info("Обновлён пользователь: {}", existingUser.getLogin());
        return existingUser;
    }

    @Override
    public User getUserById(int id) {
        return users.get(id);
    }

    @Override
    public List<User> getAllUsers() {
        log.info("Получен запрос на получение всех пользователей. Всего пользователей: {}", users.size());
        return new ArrayList<>(users.values());
    }

    @Override
    public void deleteUser(int id) {
        users.remove(id);
        log.info("Удален пользователь с ID: {}", id);
    }
}

package ru.yandex.practicum.filmorate.service;

import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserStorage userStorage;
    private final ValidationService validationService;

    public UserService(UserStorage userStorage, ValidationService validationService) {
        this.userStorage = userStorage;
        this.validationService = validationService;
    }

    public User addUser(User user) {
        validationService.validateUser(user);
        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.addUser(user);
    }


    public User updateUser(User user) {
        if (user.getId() == null) {
            throw new IllegalArgumentException("ID пользователя не может быть null при обновлении");
        }

        User existingUser = userStorage.getUserById(user.getId());
        if (existingUser == null) {
            throw new NotFoundException("Пользователь с ID " + user.getId() + " не найден");
        }

        validationService.validateUser(user);

        if (user.getName() == null || user.getName().isBlank()) {
            user.setName(user.getLogin());
        }
        return userStorage.updateUser(user);
    }

    public List<User> getAllUsers() {
        return userStorage.getAllUsers();
    }

    public User getUserById(int id) {
        User user = userStorage.getUserById(id);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + id + " не найден");
        }
        return user;
    }

    public void deleteUser(int id) {
        userStorage.deleteUser(id);
    }

    public void addFriend(int userId, int friendId) {
        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        if (friend == null) {
            throw new NotFoundException("Пользователь с ID " + friendId + " не найден");
        }

        if (user.getFriends().contains(friendId)) {
            throw new ValidationException("Пользователь уже в друзьях");
        }

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
    }

    public void removeFriend(int userId, int friendId) {
        User user = userStorage.getUserById(userId);
        User friend = userStorage.getUserById(friendId);

        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        if (friend == null) {
            throw new NotFoundException("Пользователь с ID " + friendId + " не найден");
        }

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
    }

    public List<User> getFriends(int userId) {
        User user = userStorage.getUserById(userId);
        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }

        return user.getFriends().stream()
                .map(friendId -> userStorage.getUserById(friendId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(int userId, int otherUserId) {
        User user = userStorage.getUserById(userId);
        User otherUser = userStorage.getUserById(otherUserId);

        if (user == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        if (otherUser == null) {
            throw new NotFoundException("Пользователь с ID " + otherUserId + " не найден");
        }

        Set<Integer> userFriends = user.getFriends();
        Set<Integer> otherFriends = otherUser.getFriends();

        return userFriends.stream()
                .filter(otherFriends::contains)
                .map(friendId -> userStorage.getUserById(friendId))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}


package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventStorage eventStorage;
    private final UserStorage userStorage;

    public void addEvent(int userId, EventType eventType, Operation operation, int entityId) {
        Event event = new Event();
        event.setUserId(userId);
        event.setTimestamp(System.currentTimeMillis());
        event.setEventType(eventType);
        event.setOperation(operation);
        event.setEntityId(entityId);
        eventStorage.addEvent(event);
    }

    public List<Event> getFeed(int userId) {
        if (userStorage.getUserById(userId) == null) {
            throw new NotFoundException("Пользователь с ID " + userId + " не найден");
        }
        return eventStorage.getFeed(userId);
    }
}

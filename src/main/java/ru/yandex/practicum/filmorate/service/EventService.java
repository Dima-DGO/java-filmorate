package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.model.EventType;
import ru.yandex.practicum.filmorate.model.Operation;
import ru.yandex.practicum.filmorate.storage.event.EventStorage;

@Service
@Slf4j
@RequiredArgsConstructor
public class EventService {
    private final EventStorage eventStorage;

    public void addEvent(int userId, int entityId, EventType type, Operation operation) {
        Event event = new Event();
        event.setUserId(userId);
        event.setEntityId(entityId);
        event.setEventType(type);
        event.setOperation(operation);
        event.setTimestamp(System.currentTimeMillis());
        eventStorage.add(event);
    }
}

package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Event {
    private Integer eventId;
    private Integer userId;
    private long timestamp;
    private EventType eventType;
    private Operation operation;
    private Integer entityId;
}

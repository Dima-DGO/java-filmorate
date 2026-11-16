package ru.yandex.practicum.filmorate.model;

import lombok.Data;

@Data
public class Event {
    private Integer eventId;
    private Integer userId;
    private Integer entityId;
    private EventType eventType;
    private Operation operation;
    private Long timestamp;
}

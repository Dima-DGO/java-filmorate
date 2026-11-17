package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.director.DirectorStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DirectorService {

    private static final int MAX_NAME_LENGTH = 100;

    private final DirectorStorage directorStorage;

    public List<Director> getAll() {
        return directorStorage.getAll();
    }

    public Director getById(int id) {
        Director director = directorStorage.getById(id);
        if (director == null) {
            throw new NotFoundException("Режиссёр с ID " + id + " не найден");
        }
        return director;
    }

    public Director addDirector(Director director) {
        validate(director);
        return directorStorage.addDirector(director);
    }

    public Director updateDirector(Director director) {
        if (director.getId() == null) {
            throw new ValidationException("ID режиссёра не может быть null при обновлении");
        }
        validate(director);
        getById(director.getId());
        return directorStorage.updateDirector(director);
    }

    public void deleteDirector(int id) {
        getById(id);
        directorStorage.deleteDirector(id);
    }

    private void validate(Director director) {
        if (director.getName() == null || director.getName().isBlank()) {
            throw new ValidationException("Имя режиссёра не может быть пустым");
        }
        if (director.getName().length() > MAX_NAME_LENGTH) {
            throw new ValidationException("Имя режиссёра не может быть длиннее "
                    + MAX_NAME_LENGTH + " символов");
        }
    }
}

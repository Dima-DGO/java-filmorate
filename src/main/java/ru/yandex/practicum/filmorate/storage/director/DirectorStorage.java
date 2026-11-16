package ru.yandex.practicum.filmorate.storage.director;

import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DirectorStorage {
    List<Director> getAll();

    Director getById(int id);

    Director add(Director director);

    Director update(Director director);

    void delete(int id);

    Map<Integer, Set<Director>> getByFilmIds(List<Integer> filmIds);
}

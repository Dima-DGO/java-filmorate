package ru.yandex.practicum.filmorate.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.service.DirectorService;

import java.util.List;

@RestController
@RequestMapping("/directors")
@RequiredArgsConstructor
@Slf4j
public class DirectorController {

    private final DirectorService directorService;

    @GetMapping
    public List<Director> getAll() {
        log.info("Получен запрос на получение всех режиссёров");
        return directorService.getAll();
    }

    @GetMapping("/{id}")
    public Director getById(@PathVariable int id) {
        log.info("Получен запрос на получение режиссёра с ID: {}", id);
        return directorService.getById(id);
    }

    @PostMapping
    public Director addDirector(@RequestBody Director director) {
        log.info("Получен запрос на добавление режиссёра: {}", director.getName());
        return directorService.addDirector(director);
    }

    @PutMapping
    public Director updateDirector(@RequestBody Director director) {
        log.info("Получен запрос на обновление режиссёра с ID: {}", director.getId());
        return directorService.updateDirector(director);
    }

    @DeleteMapping("/{id}")
    public void deleteDirector(@PathVariable int id) {
        log.info("Получен запрос на удаление режиссёра с ID: {}", id);
        directorService.deleteDirector(id);
    }
}

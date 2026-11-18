package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final UserStorage userStorage;
    private final FilmStorage filmStorage;
    private final UserService userService;

    public List<Film> getRecommendations(int userId) {
        log.debug("Построение рекомендаций для пользователя id={}", userId);

        // check for existance
        userService.getUserById(userId);

        Set<Integer> likedByTarget = filmStorage.getLikedFilmIds(userId);
        if (likedByTarget.isEmpty()) {
            log.debug("У User id={} нет лайков: рекомендации отсутствуют", userId);
            return List.of();
        }

        List<Integer> similarUserIds = userStorage.getSimilarUserIds(userId);
        if (similarUserIds.isEmpty()) {
            log.debug("Для User id={} не найдено Users с похожими предпочтениями: рекомендации отсутствуют", userId);
            return List.of();
        }

        List<Film> recommendations = filmStorage.getFilmsLikedByUsersExcluding(similarUserIds, userId);

        log.debug("Для User id={} найдено {} рекомендаций, Users со схожими предпочтениями найдено: {})", userId,
                recommendations.size(), similarUserIds.size());

        return recommendations;
    }
}
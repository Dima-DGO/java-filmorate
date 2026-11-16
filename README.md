# java-filmorate
Template repository for Filmorate project.
| Фича | Эндпоинты |
|---|---|
| Отзывы | `POST /reviews`, `PUT /reviews`, `DELETE /reviews/{id}`, `GET /reviews/{id}`, `GET /films/{id}/reviews?sort=useful\|date`, `PUT/DELETE /reviews/{id}/like/{userId}`, `PUT/DELETE /reviews/{id}/dislike/{userId}` |
| Поиск | `GET /films/search?query=&by=title,director` |
| Общие фильмы | `GET /films/common?userId=&friendId=` |
| Рекомендации | `GET /users/{id}/recommendations` |
| Лента событий | `GET /users/{id}/feed` |
| Удаление | `DELETE /films/{id}`, `DELETE /users/{id}` |
| Режиссёры | `GET /directors`, `GET /directors/{id}`, `POST /directors`, `PUT /directors`, `DELETE /directors/{id}`, `GET /films/director/{id}?sortBy=year\|likes` |
| Популярные фильмы | `GET /films/popular?count=&genreId=&year=` |
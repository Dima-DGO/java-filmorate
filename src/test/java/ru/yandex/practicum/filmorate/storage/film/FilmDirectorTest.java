package ru.yandex.practicum.filmorate.storage.film;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.MpaRating;
import ru.yandex.practicum.filmorate.storage.director.DirectorDbStorage;

import java.time.LocalDate;
import java.util.Set;
import java.util.HashSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import({FilmDbStorage.class, DirectorDbStorage.class})
class FilmDirectorTest {

    private final FilmDbStorage filmStorage;
    private final DirectorDbStorage directorStorage;

    @Autowired
    public FilmDirectorTest(FilmDbStorage filmStorage, DirectorDbStorage directorStorage) {
        this.filmStorage = filmStorage;
        this.directorStorage = directorStorage;
    }

    private Director makeDirector(String name) {
        Director director = new Director();
        director.setName(name);
        return directorStorage.addDirector(director);
    }

    private Film makeFilm(Set<Director> directors) {
        Film film = new Film();
        film.setName("Inception");
        film.setDescription("Сон во сне");
        film.setReleaseDate(LocalDate.of(2010, 7, 16));
        film.setDuration(148);
        MpaRating mpa = new MpaRating();
        mpa.setId(1);
        film.setMpa(mpa);
        Genre genre = new Genre();
        genre.setId(1);
        film.setGenres(Set.of(genre));
        film.setDirectors(directors);
        return film;
    }

    @Test
    void addFilmSavesDirectors() {
        Director director = makeDirector("Christopher Nolan");

        Film created = filmStorage.addFilm(makeFilm(Set.of(director)));
        Film found = filmStorage.getFilmById(created.getId());

        assertThat(found.getDirectors()).hasSize(1);
        assertThat(found.getDirectors().iterator().next())
                .hasFieldOrPropertyWithValue("id", director.getId())
                .hasFieldOrPropertyWithValue("name", "Christopher Nolan");
    }

    @Test
    void filmWithoutDirectorsHasEmptySet() {
        Film created = filmStorage.addFilm(makeFilm(Set.of()));

        Film found = filmStorage.getFilmById(created.getId());

        assertThat(found.getDirectors()).isNotNull().isEmpty();
    }

    @Test
    void duplicateDirectorIdsAreNotDuplicated() {
        Director first = makeDirector("Christopher Nolan");
        Director same = new Director();
        same.setId(first.getId());
        same.setName(first.getName());

        Set<Director> withDuplicates = new HashSet<>();
        withDuplicates.add(first);
        withDuplicates.add(same);

        Film created = filmStorage.addFilm(makeFilm(withDuplicates));
        Film found = filmStorage.getFilmById(created.getId());

        assertThat(found.getDirectors()).hasSize(1);
    }

    @Test
    void updateFilmReplacesDirectors() {
        Director nolan = makeDirector("Christopher Nolan");
        Director tarantino = makeDirector("Quentin Tarantino");
        Film created = filmStorage.addFilm(makeFilm(Set.of(nolan)));

        created.setDirectors(Set.of(tarantino));
        filmStorage.updateFilm(created);

        Film found = filmStorage.getFilmById(created.getId());
        assertThat(found.getDirectors()).hasSize(1);
        assertThat(found.getDirectors().iterator().next().getId()).isEqualTo(tarantino.getId());
    }

    @Test
    void updateFilmCanRemoveAllDirectors() {
        Director nolan = makeDirector("Christopher Nolan");
        Film created = filmStorage.addFilm(makeFilm(Set.of(nolan)));

        created.setDirectors(Set.of());
        filmStorage.updateFilm(created);

        Film found = filmStorage.getFilmById(created.getId());
        assertThat(found.getDirectors()).isEmpty();
    }

    @Test
    void getAllFilmsLoadsDirectors() {
        Director nolan = makeDirector("Christopher Nolan");
        Film created = filmStorage.addFilm(makeFilm(Set.of(nolan)));

        Film found = filmStorage.getAllFilms().stream()
                .filter(f -> f.getId().equals(created.getId()))
                .findFirst().orElseThrow();

        assertThat(found.getDirectors()).hasSize(1);
    }

    @Test
    void unknownDirectorThrowsNotFound() {
        Director unknown = new Director();
        unknown.setId(9999);

        assertThatThrownBy(() -> filmStorage.addFilm(makeFilm(Set.of(unknown))))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("не найден");
    }

    @Test
    void deletingDirectorDoesNotDeleteFilm() {
        Director nolan = makeDirector("Christopher Nolan");
        Film created = filmStorage.addFilm(makeFilm(Set.of(nolan)));

        directorStorage.deleteDirector(nolan.getId());

        Film found = filmStorage.getFilmById(created.getId());
        assertThat(found).isNotNull();
        assertThat(found.getDirectors()).isEmpty();
    }
}

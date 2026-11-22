package ru.yandex.practicum.filmorate.storage.director;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Director;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@AutoConfigureTestDatabase
@Import(DirectorDbStorage.class)
class DirectorDbStorageTest {

    private final DirectorDbStorage directorStorage;

    @Autowired
    public DirectorDbStorageTest(DirectorDbStorage directorStorage) {
        this.directorStorage = directorStorage;
    }

    private Director makeDirector(String name) {
        Director director = new Director();
        director.setName(name);
        return director;
    }

    @Test
    void addDirectorAssignsId() {
        Director created = directorStorage.addDirector(makeDirector("Christopher Nolan"));

        assertThat(created.getId()).isNotNull().isPositive();
        assertThat(directorStorage.getById(created.getId()))
                .hasFieldOrPropertyWithValue("name", "Christopher Nolan");
    }

    @Test
    void getByIdReturnsNullForUnknownId() {
        assertThat(directorStorage.getById(9999)).isNull();
    }

    @Test
    void getAllReturnsCreatedDirectors() {
        Director created = directorStorage.addDirector(makeDirector("Quentin Tarantino"));

        assertThat(directorStorage.getAll())
                .extracting(Director::getId)
                .contains(created.getId());
    }

    @Test
    void updateDirectorChangesName() {
        Director created = directorStorage.addDirector(makeDirector("Old name"));
        created.setName("New name");

        directorStorage.updateDirector(created);

        assertThat(directorStorage.getById(created.getId()))
                .hasFieldOrPropertyWithValue("name", "New name");
    }

    @Test
    void updateUnknownDirectorThrowsNotFound() {
        Director unknown = makeDirector("Ghost");
        unknown.setId(9999);

        assertThatThrownBy(() -> directorStorage.updateDirector(unknown))
                .hasMessageContaining("не найден");
    }

    @Test
    void deleteDirectorRemovesDirector() {
        Director created = directorStorage.addDirector(makeDirector("Temporary"));

        directorStorage.deleteDirector(created.getId());

        assertThat(directorStorage.getById(created.getId())).isNull();
    }

    @Test
    void getAllContainsDirectorByNameIgnoringCase() {
        directorStorage.addDirector(makeDirector("Christopher Nolan"));

        List<Director> found = directorStorage.getAll().stream()
                .filter(director -> director.getName().equalsIgnoreCase("christopher nolan"))
                .toList();

        assertThat(found).hasSize(1);
        assertThat(found.get(0).getName()).isEqualTo("Christopher Nolan");
    }
}

package ru.yandex.practicum.filmorate.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class DirectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private int createDirector(String name) throws Exception {
        String response = mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isNumber())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asInt();
    }

    @Test
    void createAndGetDirectorById() throws Exception {
        int id = createDirector("Christopher Nolan");

        mockMvc.perform(get("/directors/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value("Christopher Nolan"));
    }

    @Test
    void getAllDirectorsContainsCreated() throws Exception {
        int id = createDirector("Quentin Tarantino");

        mockMvc.perform(get("/directors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + id + ")]").exists());
    }

    @Test
    void createDirectorWithBlankNameReturns400() throws Exception {
        mockMvc.perform(post("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void getUnknownDirectorReturns404() throws Exception {
        mockMvc.perform(get("/directors/{id}", 999999))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateDirectorChangesName() throws Exception {
        int id = createDirector("Old name");

        mockMvc.perform(put("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":" + id + ",\"name\":\"New name\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("New name"));

        mockMvc.perform(get("/directors/{id}", id))
                .andExpect(jsonPath("$.name").value("New name"));
    }

    @Test
    void updateDirectorWithoutIdReturns400() throws Exception {
        mockMvc.perform(put("/directors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"No id\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteDirectorRemovesIt() throws Exception {
        int id = createDirector("Temporary");

        mockMvc.perform(delete("/directors/{id}", id))
                .andExpect(status().isOk());

        mockMvc.perform(get("/directors/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteUnknownDirectorReturns404() throws Exception {
        mockMvc.perform(delete("/directors/{id}", 999999))
                .andExpect(status().isNotFound());
    }
}

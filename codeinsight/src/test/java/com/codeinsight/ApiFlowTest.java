package com.codeinsight;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiFlowTest {
    @Autowired
    MockMvc mvc;

    private String uniq() { return "u" + UUID.randomUUID().toString().substring(0, 8); }

    private String register(String user) throws Exception {
        String body = "{\"username\":\"" + user + "\",\"password\":\"secret1\"}";
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body)).andExpect(status().isOk());
        return body;
    }

    private String login(String user) throws Exception {
        String res = mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(register(user)))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(res, "$.token");
    }

    @Test
    void protectedRouteRejectsMissingToken() throws Exception {
        mvc.perform(get("/api/problems")).andExpect(status().isUnauthorized());
    }

    @Test
    void duplicateUsernameIsRejected() throws Exception {
        String user = uniq();
        String body = register(user);
        mvc.perform(post("/api/auth/register").contentType(APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
    }

    @Test
    void wrongPasswordIsRejectedAndRepeatedFailuresAreRateLimited() throws Exception {
        String user = uniq();
        register(user);
        String bad = "{\"username\":\"" + user + "\",\"password\":\"wrong\"}";
        for (int i = 0; i < 5; i++)
            mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(bad)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(APPLICATION_JSON).content(bad)).andExpect(status().isTooManyRequests());
    }

    @Test
    void problemCrudAndStatsFlow() throws Exception {
        String auth = login(uniq());
        String problem = "{\"title\":\"Two Sum\",\"platform\":\"LeetCode\",\"difficulty\":\"Easy\",\"topic\":\"Array\",\"status\":\"Solved\"}";

        String created = mvc.perform(post("/api/problems").header("Authorization", auth).contentType(APPLICATION_JSON).content(problem))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solvedDate").exists())        // set automatically
                .andExpect(jsonPath("$.nextReviewDate").exists())    // spaced repetition scheduled
                .andReturn().getResponse().getContentAsString();
        int id = JsonPath.read(created, "$.id");

        mvc.perform(get("/api/stats").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.solved").value(1))
                .andExpect(jsonPath("$.currentStreak").value(1));

        mvc.perform(delete("/api/problems/" + id).header("Authorization", auth)).andExpect(status().isOk());
        mvc.perform(get("/api/problems").header("Authorization", auth)).andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invalidDifficultyIsRejected() throws Exception {
        String auth = login(uniq());
        mvc.perform(post("/api/problems").header("Authorization", auth).contentType(APPLICATION_JSON)
                .content("{\"title\":\"X\",\"difficulty\":\"Impossible\"}")).andExpect(status().isBadRequest());
    }

    @Test
    void usersCannotSeeEachOthersProblems() throws Exception {
        String a = login(uniq()), b = login(uniq());
        mvc.perform(post("/api/problems").header("Authorization", a).contentType(APPLICATION_JSON)
                .content("{\"title\":\"Private\",\"difficulty\":\"Easy\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/problems").header("Authorization", b)).andExpect(jsonPath("$.length()").value(0));
    }
}

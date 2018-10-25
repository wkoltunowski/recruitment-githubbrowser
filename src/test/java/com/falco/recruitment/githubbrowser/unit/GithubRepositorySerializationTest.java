package com.falco.recruitment.githubbrowser.unit;

import com.falco.recruitment.githubbrowser.integration.GithubRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GithubRepositorySerializationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldDeserializeFromJSon() throws Exception {
        String json = new JSONObject()
                .put("name", "spring-boot")
                .put("full_name", "Spring Boot")
                .put("description", "wokol repository for Spring Boot")
                .put("stargazers_count", 123)
                .put("created_at", "2017-01-01T12:15:15")
                .put("clone_url", "http://clone.url")
                .put("html_url", "https://github.com/wokol")
                .put("owner", new JSONObject().put("login", "wokol").put("id", 20225750))
                .toString();
        GithubRepository githubRepository = objectMapper.readValue(json, GithubRepository.class);
        assertThat(githubRepository).isEqualTo(
                GithubRepository.builder()
                        .name("spring-boot")
                        .fullName("Spring Boot")
                        .description("wokol repository for Spring Boot")
                        .stars(123)
                        .createdAt(LocalDateTime.parse("2017-01-01T12:15:15"))
                        .cloneUrl("http://clone.url")
                        .owner(GithubRepository.login("wokol"))
                        .build()
        );
    }

    @Test
    public void shouldSerializeToJSon() throws Exception {
        GithubRepository repository = GithubRepository.builder()
                .name("spring-boot")
                .fullName("Spring Boot")
                .description("wokol repository for Spring Boot")
                .stars(123)
                .createdAt(LocalDateTime.parse("2017-01-01T12:15:15"))
                .cloneUrl("http://clone.url")
                .owner(GithubRepository.login("wokol"))
                .build();
        String json = objectMapper.writeValueAsString(repository);
        assertEquals(
                new JSONObject(json),
                new JSONObject()
                        .put("name", "spring-boot")
                        .put("full_name", "Spring Boot")
                        .put("description", "wokol repository for Spring Boot")
                        .put("stargazers_count", 123)
                        .put("created_at", "2017-01-01T12:15:15")
                        .put("clone_url", "http://clone.url")
                        .put("html_url", "https://github.com/wokol")
                        .put("owner", new JSONObject().put("login", "wokol").put("id", 20225750)));
    }

    private static void assertEquals(JSONObject repositoryJSon, JSONObject expected) throws JSONException {
        JSONAssert.assertEquals(repositoryJSon, expected, false);
    }
}

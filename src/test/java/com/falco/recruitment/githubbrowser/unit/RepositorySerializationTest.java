package com.falco.recruitment.githubbrowser.unit;

import com.falco.recruitment.githubbrowser.Repository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.Condition;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RepositorySerializationTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void shouldSerializeToJSon() throws Exception {
        String repositoryJSon = objectMapper.writeValueAsString(Repository.builder()
                .name("spring-boot")
                .fullName("Spring Boot")
                .description("wokol repository for Spring Boot")
                .stars(123)
                .createdAt(LocalDateTime.parse("2017-01-01T12:15:15"))
                .cloneUrl("http://clone.url")
                .owner("wokol")
                .eTag(ETag.from("abc"))
                .build());
        assertEquals(
                new JSONObject(repositoryJSon),
                new JSONObject().
                        put("name", "spring-boot").
                        put("fullName", "Spring Boot").
                        put("description", "wokol repository for Spring Boot").
                        put("stars", 123).
                        put("createdAt", "2017-01-01T12:15:15").
                        put("cloneUrl", "http://clone.url").
                        put("owner", "wokol"));
    }

    @Test
    public void shouldDeserializeFromJSon() throws Exception {
        JSONObject jsonObject = new JSONObject().
                put("name", "spring-boot").
                put("fullName", "Spring Boot").
                put("description", "wokol repository for Spring Boot").
                put("stars", 123).
                put("createdAt", "2017-01-01T12:15:15").
                put("cloneUrl", "http://clone.url").
                put("owner", "wokol");
        Repository repository = objectMapper.readValue(jsonObject.toString(), Repository.class);

        assertThat(repository).isEqualTo(Repository.builder()
                .name("spring-boot")
                .fullName("Spring Boot")
                .description("wokol repository for Spring Boot")
                .stars(123)
                .createdAt(LocalDateTime.parse("2017-01-01T12:15:15"))
                .cloneUrl("http://clone.url")
                .owner("wokol")
                .build());
    }

    private static void assertEquals(JSONObject actual, JSONObject expected) throws JSONException {
        JSONAssert.assertEquals(expected, actual, true);
    }

    @Test
    public void shouldIgnoreETag() throws Exception {
        String repositoryJSon = objectMapper.writeValueAsString(Repository.builder()
                .name("spring-boot")
                .owner("wokol")
                .eTag(ETag.from("abc"))
                .build());
        assertThat(new JSONObject(repositoryJSon)).doesNotHave(hasProperty("eTag"));
    }

    @Test
    public void shouldSerializeDateToISO() throws Exception {
        String repositoryJSon = objectMapper.writeValueAsString(Repository.builder()
                .name("spring-boot")
                .owner("wokol")
                .createdAt(LocalDateTime.parse("2017-01-01T12:15:15"))
                .build());
        assertThat(new JSONObject(repositoryJSon).get("createdAt")).isEqualTo("2017-01-01T12:15:15");
    }

    private Condition<JSONObject> hasProperty(final String property) {
        return new Condition<JSONObject>() {
            @Override
            public boolean matches(JSONObject jsonObject) {
                return jsonObject.has(property);
            }
        };
    }
}

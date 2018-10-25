package com.falco.recruitment.githubbrowser.integration;

import com.falco.recruitment.githubbrowser.Repository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class RepositoriesControllerIntegrationTest {
    @Value("http://localhost:${local.server.port}${githubbrowser.repositories.url}")
    private String repositoriesUrl;
    @Value("${github.url}" + "/repositories")
    private String githubAllRepositoriesUrl;

    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        restTemplate = new RestTemplate();
    }

    @Test
    public void shouldGetUserRepositoryDetails() {
        GithubRepository firstRepository = get(githubAllRepositoriesUrl, GithubRepository[].class).getBody()[0];

        ResponseEntity<Repository> response = get(repositoriesUrl, Repository.class, firstRepository.getOwner().getLogin(), firstRepository.getName());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getOwner()).isNotEmpty();
    }

    private <T> ResponseEntity<T> get(String url, Class<T> responseType, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), responseType, uriVariables);
    }
}
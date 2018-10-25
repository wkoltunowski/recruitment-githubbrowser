package com.falco.recruitment.githubbrowser.integration;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import static com.falco.recruitment.githubbrowser.integration.GithubRepositoryFinder.GITHUB_REPOSITORIES_URL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test checks if all assumptions regarding github api are met.
 * If anything in github.api changes important for githubbrowser application this test should detect it.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class GithubExpectationsTest {
    @Value("${github.url}" + "/repositories")
    private String githubAllRepositoriesUrl;
    @Value(GITHUB_REPOSITORIES_URL)
    private String githubRepositoryUrl;
    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void shouldGetUserRepositoryDetails() {
        GithubRepository firstRepository = get(githubAllRepositoriesUrl, GithubRepository[].class).getBody()[0];
        ResponseEntity<GithubRepository> response = getGithubRepositoryFor(GithubRepository.class, firstRepository.getOwner().getLogin(), firstRepository.getName());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        GithubRepository repository = response.getBody();
        assertThat(repository.getOwner().getLogin()).isNotEmpty();
        assertThat(repository.getName()).isNotEmpty();
        assertThat(repository.getCloneUrl()).isNotEmpty();
        assertThat(repository.getDescription()).isNotEmpty();
        assertThat(repository.getFullName()).isNotEmpty();
        assertThat(repository.getCreatedAt()).isNotNull();
        assertThat(repository.getStars()).isGreaterThan(-1);
    }

    @Test
    public void shouldReturn404ForNotExistingRepository() {
        ResponseEntity<String> response = getGithubRepositoryFor(String.class, "nonExistingLogin", "nonExistingRepository");
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private <T> ResponseEntity<T> getGithubRepositoryFor(Class<T> tClass, String owner, String repository) {
        return get(githubRepositoryUrl, tClass, owner, repository);
    }

    private <T> ResponseEntity<T> get(String url, Class<T> responseType, Object... uriVariables) {
        return restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), responseType, uriVariables);
    }
}
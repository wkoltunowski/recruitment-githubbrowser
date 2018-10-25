package com.falco.recruitment.githubbrowser.unit;

import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.integration.GithubRepository;
import com.falco.recruitment.githubbrowser.integration.GithubRepositoryFinder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static com.falco.recruitment.githubbrowser.OwnerRepository.ownerRepository;
import static com.falco.recruitment.githubbrowser.integration.GithubRepository.login;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GithubRepositoryFinderTest {
    private MockRestServiceServer mockServer;
    private GithubRepositoryFinder githubRepositoryFinder;
    private final String githubReposUrl = "http://localhost/repositories/{owner}/{repository}";
    @Autowired
    private RestTemplate restTemplate;
    @Autowired
    private ObjectMapper objectMapper;


    @Before
    public void setUp() {
        mockServer = MockRestServiceServer.createServer(restTemplate);
        githubRepositoryFinder = new GithubRepositoryFinder(githubReposUrl, restTemplate);
    }

    @Test
    public void shouldReturnEmptyForNonexistentRepository() {
        mockServer.expect(requestTo(urlFor("owner", "name")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));
        assertThat(githubRepositoryFinder.tryFind(ownerRepository("owner", "name"), ETag.NO_ETAG)).isEqualTo(Optional.empty());
    }

    @Test
    public void shouldReturnRepository() throws JsonProcessingException {
        String json = objectMapper.writeValueAsString(GithubRepository.builder().owner(login("owner")).name("name").build());
        mockServer.expect(requestTo(urlFor("owner", "name")))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.OK).contentType(MediaType.APPLICATION_JSON).
                        headers(eTag(ETag.from("abc"))).
                        body(json));
        assertThat(githubRepositoryFinder.tryFind(ownerRepository("owner", "name"), ETag.NO_ETAG)).isEqualTo(
                Optional.of(Repository.builder().owner("owner").name("name").eTag(ETag.from("abc")).build()));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldRaiseExceptionWhenNoContentForExistingRepository() {
        mockServer.expect(requestTo(urlFor("owner", "name")))
                .andExpect(method(HttpMethod.GET))
                .andExpect(header(HttpHeaders.ETAG, ETag.from("abc").toString()))
                .andRespond(withStatus(HttpStatus.NOT_MODIFIED).
                        headers(eTag(ETag.from("abc"))));
        githubRepositoryFinder.tryFind(ownerRepository("owner", "name"), ETag.from("abc"));
    }

    private HttpHeaders eTag(ETag eTag) {
        HttpHeaders headers = new HttpHeaders();
        eTag.addTo(headers);
        return headers;
    }

    private String urlFor(String owner, String repository) {
        return githubReposUrl.replace("{owner}", owner).replace("{repository}", repository);
    }
}
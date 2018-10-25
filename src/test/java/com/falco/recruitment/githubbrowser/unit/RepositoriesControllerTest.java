package com.falco.recruitment.githubbrowser.unit;

import com.falco.recruitment.githubbrowser.RepositoriesController;
import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.RepositoryFinder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;

import java.util.stream.Stream;

import static com.falco.recruitment.githubbrowser.RepositoriesController.REPOSITORIES_CACHE_MAX_AGE_SECONDS;
import static com.falco.recruitment.githubbrowser.RepositoriesController.REPOSITORIES_URL;
import static com.falco.recruitment.githubbrowser.Repository.builder;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@WebMvcTest(RepositoriesController.class)
public class RepositoriesControllerTest {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Value(REPOSITORIES_CACHE_MAX_AGE_SECONDS)
    private int cacheMaxAgeInSeconds;
    @Value(REPOSITORIES_URL)
    private String repositoriesUrl;

    @MockBean
    private RepositoryFinder repositoryFinder;

    @Test
    public void shouldFindRepository() throws Exception {
        Repository repository = repository("owner", "name").build();
        givenRepositories(repository);

        doGET("owner", "name", new HttpHeaders()).
                andDo(print()).
                andExpect(status().isOk()).
                andExpect(content().json(toJSon(repository), true));
    }

    @Test
    public void shouldReturn404WhenNoRepositoryFound() throws Exception {
        givenRepositories();
        doGET("owner", "repository", new HttpHeaders()).
                andDo(print()).
                andExpect(status().isNotFound()).
                andExpect(noContent());
    }

    @Test
    public void shouldAllowConditionalGet() throws Exception {
        Repository repositoryWithETag = repository("owner", "name").eTag(ETag.from("abc")).build();
        givenRepositories(repositoryWithETag);

        ETag repositoryETag = ETag.from(doGET("owner", "name", new HttpHeaders()).andReturn().getResponse().getHeader(HttpHeaders.ETAG));
        assertThat(repositoryETag.toString()).isNotEmpty();

        doGET("owner", "name", headers(repositoryETag)).
                andDo(print()).
                andExpect(status().isNotModified()).
                andExpect(noContent());
    }

    @Test
    public void shouldAllowCaching() throws Exception {
        Repository repository = repository("owner", "name").build();
        givenRepositories(repository);

        doGET("owner", "name", new HttpHeaders()).
                andDo(print()).
                andExpect(header().string("cache-control", String.format("max-age=%d, public", cacheMaxAgeInSeconds)));
    }

    private ResultMatcher noContent() {
        return content().string("");
    }

    private String toJSon(Repository repository) throws JsonProcessingException {
        return objectMapper.writeValueAsString(repository);
    }

    private ResultActions doGET(String owner, String name, HttpHeaders headers) throws Exception {
        return mvc.perform(get(repositoriesUrl, owner, name).
                contentType(APPLICATION_JSON).
                headers(headers));
    }

    private Repository.RepositoryBuilder repository(String owner, String name) {
        return builder().owner(owner).name(name);
    }

    private HttpHeaders headers(ETag eTag) {
        HttpHeaders headers = new HttpHeaders();
        eTag.addTo(headers);
        return headers;
    }

    private void givenRepositories(Repository... repositories) {
        when(repositoryFinder.tryFind(any(), any())).thenReturn(empty());
        Stream.of(repositories).forEach(this::mockFinderFor);
    }

    private void mockFinderFor(Repository repository) {
        when(repositoryFinder.tryFind(repository.ownerRepository(), repository.getETag())).thenReturn(of(repository));
        when(repositoryFinder.tryFind(repository.ownerRepository(), ETag.NO_ETAG)).thenReturn(of(repository));
    }
}
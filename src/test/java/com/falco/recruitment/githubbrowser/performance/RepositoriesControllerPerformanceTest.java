package com.falco.recruitment.githubbrowser.performance;

import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.integration.GithubRepository;
import com.google.common.base.Stopwatch;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.falco.recruitment.githubbrowser.integration.GithubRepository.builder;
import static com.falco.recruitment.githubbrowser.integration.GithubRepository.login;
import static org.apache.commons.lang3.ObjectUtils.median;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * This is very naive test checking how many requests application can serve with mocked (over http )github api (GithubMock).
 * May be useful for initial http client connection factory parameters configuration.
 * It would make sense to extract it into separate maven project using specialized library (eg. https://gatling.io/).
 */
@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@TestPropertySource(locations = {
        "classpath:github.mock.properties",
        "classpath:no.logging.properties",
})
public class RepositoriesControllerPerformanceTest {
    @Autowired
    private GithubReposMock githubMock;
    @Value("http://localhost:${local.server.port}${githubbrowser.repositories.url}")
    private String repositoriesUrl;
    @Autowired
    private RestTemplate restTemplate;
    private List<GithubRepository> repositories = Collections.emptyList();
    private static final int totalRepositoryCount = 300;
    private static final int REPOSITORIES_COUNT = totalRepositoryCount;
    private static int lastIndex;

    @Before
    public void warmUp() {
        addNRepositories(10);
        runNRequests(500);
    }

    @Test
    public void shouldServe100ReqPerSecondWithoutCaching() {
        assertThat(calculateTPSForCacheFactor(0.0001f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond10PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.1f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond30PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.3f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond50PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.5f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond70PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.7f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond80PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.8f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond90PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.9f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond95PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.95f)).isGreaterThan(100);
    }

    @Test
    public void shouldServe100ReqPerSecond99PercentCached() {
        assertThat(calculateTPSForCacheFactor(0.99f)).isGreaterThan(100);
    }


    @Test
    public void shouldServe100ReqPerSecond100PercentCached() {
        assertThat(calculateTPSForCacheFactor(1f)).isGreaterThan(100);
    }

    private int calculateTPSForCacheFactor(float cacheFactor) {
        Stream<Float> tpsStream = IntStream.range(0, 3).mapToObj(i -> tpsForCacheFactor(cacheFactor));
        Float median = median(tpsStream.toArray(i -> new Float[3]));
        System.out.printf("_____________________________median %.2f tps%n", median);
        return Math.round(median);
    }

    private float tpsForCacheFactor(float cacheFactor) {
        addNRepositories(REPOSITORIES_COUNT);
        //fill cache with repositories for given factor
        runNRequests(cacheFactor * REPOSITORIES_COUNT);

        Stopwatch stopwatch = Stopwatch.createStarted();
        runNRequests(REPOSITORIES_COUNT);
        float executionTimeInMillis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
        float tps = REPOSITORIES_COUNT / executionTimeInMillis * 1000f;
        int cachePercentage = Math.round(cacheFactor * 100f);
        System.out.printf("Cache hit ratio of %3d%% performs at %4.2f tps (repositories range [%d;%d))%n",
                cachePercentage, tps, lastIndex - REPOSITORIES_COUNT, lastIndex);
        return tps;
    }

    private void runNRequests(float requests) {
        IntStream.range(0, (int) requests - 1).forEach(i -> assertThat(getRepository(repositories.get(i % repositories.size())).getStatusCode()).isEqualTo(HttpStatus.OK));
    }

    private void addNRepositories(int totalRepositoryCount) {
        addRepositoriesFromTo(lastIndex, lastIndex + totalRepositoryCount);
        lastIndex += totalRepositoryCount;
    }

    private static GithubRepository repoNo(int no) {
        return builder().
                name("spring-boot" + no).
                owner(login("wokol" + no)).
                fullName("wokol/spring-boot" + no).
                description("Spring Boot" + no).
                cloneUrl("https://github.com/wokol/spring-boot.git" + no).
                stars(12).
                createdAt(LocalDateTime.parse("2016-07-27T07:24:27")).
                build();
    }


    private void addRepositoriesFromTo(int from, int totalRepositoryCount) {
        repositories = IntStream.range(from, totalRepositoryCount - 1).boxed().map(RepositoriesControllerPerformanceTest::repoNo).collect(Collectors.toList());
        githubMock.givenRepositories(repositories);
    }


    private ResponseEntity<Repository> getRepository(GithubRepository repo) {
        return restTemplate.exchange(repositoriesUrl, HttpMethod.GET, new HttpEntity<>(new HttpHeaders()), Repository.class, repo.getOwner().getLogin(), repo.getName());
    }
}
package com.falco.recruitment.githubbrowser.integration;

import com.falco.recruitment.githubbrowser.OwnerRepository;
import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.RepositoryFinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service(GithubRepositoryFinder.GITHUB_REPOSITORY_FINDER)
public class GithubRepositoryFinder implements RepositoryFinder {
    public static final String GITHUB_REPOSITORIES_URL = "${github.repositories.url}";
    public static final String GITHUB_REPOSITORY_FINDER = "githubRepositoryFinder";
    private String githubReposUrl;
    private RestTemplate cachingRestTemplate;

    public GithubRepositoryFinder(
            @Value(GITHUB_REPOSITORIES_URL) String githubReposUrl,
            @Autowired RestTemplate cachingRestTemplate) {
        this.githubReposUrl = githubReposUrl;
        this.cachingRestTemplate = cachingRestTemplate;
    }

    @Override
    public Optional<Repository> tryFind(OwnerRepository ownerRepository, ETag eTag) {
        LOG.trace("Calling Github repository:'{}', for:'{}' using eTag:'{}'", githubReposUrl, ownerRepository, eTag);
        ResponseEntity<GithubRepository> githubResponse = cachingRestTemplate.exchange(
                githubReposUrl, HttpMethod.GET, eTagEntity(eTag), GithubRepository.class,
                ownerRepository.getOwner(), ownerRepository.getRepository());
        LOG.debug("Received Github response status :'{}'", githubResponse.getStatusCode());
        switch (githubResponse.getStatusCode()) {
            case OK:
                return handleOK(githubResponse);
            case NOT_FOUND:
                return handleNotFound(githubResponse);
            default:
                throw new IllegalStateException("Only OK and NOT_FOUND statuses handled in finder. Other status eg. NOT_MODIFIED should be handled by underlying cache.");
        }


    }

    private Optional<Repository> handleNotFound(ResponseEntity<GithubRepository> githubResponse) {
        return Optional.empty();
    }

    private Optional<Repository> handleOK(ResponseEntity<GithubRepository> githubResponse) {
        GithubRepository githubRepository = githubResponse.getBody();
        LOG.trace("Returning github repository '{}'", githubRepository);
        return Optional.of(Repository.builder().
                name(githubRepository.getName()).
                fullName(githubRepository.getFullName()).
                cloneUrl(githubRepository.getCloneUrl()).
                createdAt(githubRepository.getCreatedAt()).
                description(githubRepository.getDescription()).
                owner(githubRepository.getOwner().getLogin()).
                stars(githubRepository.getStars()).
                eTag(ETag.from(githubResponse.getHeaders().getETag())).
                build());
    }

    private HttpEntity<?> eTagEntity(ETag eTag) {
        HttpHeaders requestHeaders = new HttpHeaders();
        eTag.addTo(requestHeaders);
        return new HttpEntity<>(requestHeaders);
    }
}

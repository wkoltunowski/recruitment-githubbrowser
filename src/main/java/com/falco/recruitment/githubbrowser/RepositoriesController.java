package com.falco.recruitment.githubbrowser;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

import static com.falco.recruitment.githubbrowser.OwnerRepository.ownerRepository;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.springframework.http.HttpStatus.NOT_MODIFIED;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@RestController
@RequestMapping(path = RepositoriesController.REPOSITORIES_URL)
public class RepositoriesController {
    public static final String REPOSITORIES_URL = "${githubbrowser.repositories.url}";
    public static final String REPOSITORIES_CACHE_MAX_AGE_SECONDS = "${githubbrowser.repositories.cache.max.age.seconds}";
    private static final Logger LOG = org.slf4j.LoggerFactory.getLogger(RepositoriesController.class);
    private final CacheControl cacheControl;
    private final RepositoryFinder repositoryFinder;
    private final int cacheMaxAge;

    public RepositoriesController(
            @Autowired RepositoryFinder repositoryFinder,
            @Value(REPOSITORIES_CACHE_MAX_AGE_SECONDS) int cacheMaxAgeInSeconds) {
        this.repositoryFinder = repositoryFinder;
        this.cacheMaxAge = cacheMaxAgeInSeconds;
        this.cacheControl = CacheControl.maxAge(cacheMaxAge, SECONDS).cachePublic();
    }

    @RequestMapping(method = GET)
    public ResponseEntity<Repository> repositories(@RequestHeader HttpHeaders headers,
                                                   @PathVariable("owner") String owner,
                                                   @PathVariable("repository") String repository) {
        return findByETag(ownerRepository(owner, repository), etagIn(headers));
    }

    private ETag etagIn(HttpHeaders headers) {
        return ETag.from(headers.getETag());
    }

    private ResponseEntity<Repository> findByETag(OwnerRepository ownerRepository, ETag requestETag) {
        LOG.trace("Searching for repository '{}', by ETag:'{}'", ownerRepository, requestETag);
        Optional<Repository> repositoryOptional = repositoryFinder.tryFind(ownerRepository, requestETag);
        return repositoryOptional
                .map(repository -> handleFound(requestETag, repository))
                .orElseGet(this::handleNotFound);

    }

    private ResponseEntity<Repository> handleFound(ETag requestETag, Repository repository) {
        boolean notModified = sameETags(requestETag, repository);
        if (notModified) {
            ResponseEntity<Repository> response = ResponseEntity
                    .status(NOT_MODIFIED)
                    .eTag(repository.getETag().toString())
                    .cacheControl(cacheControl)
                    .build();
            LOG.trace("Repository matching etag found, returning '{}'", response);
            return response;
        }
        ResponseEntity<Repository> response = ResponseEntity
                .status(OK)
                .eTag(repository.getETag().toString())
                .cacheControl(cacheControl)
                .body(repository);
        LOG.trace("Repository found, returning '{}'", response);
        return response;
    }

    private boolean sameETags(ETag requestETag, Repository repository) {
        return sameETags(requestETag, repository.getETag());
    }

    private boolean sameETags(ETag eTag1, ETag eTag2) {
        return !eTag1.equals(ETag.NO_ETAG) && !eTag2.equals(ETag.NO_ETAG) && eTag1.equals(eTag2);
    }

    private ResponseEntity<Repository> handleNotFound() {
        ResponseEntity<Repository> response = ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        LOG.trace("Repository not found, returning '{}'", response);
        return response;
    }
}

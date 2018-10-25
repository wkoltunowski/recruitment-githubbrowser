package com.falco.recruitment.githubbrowser.performance;

import com.falco.recruitment.githubbrowser.integration.GithubRepository;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static org.apache.commons.lang.builder.ToStringBuilder.reflectionToString;
import static org.apache.commons.lang.builder.ToStringStyle.SHORT_PREFIX_STYLE;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

@ContextConfiguration
@RestController
@RequestMapping(path = "${github.repositories}")
public class GithubReposMock {
    private final MessageDigest md5;
    private Map<String, Map<String, GithubRepository>> repositories = new HashMap<>();
    private List<HttpServletRequest> requests = new ArrayList<>();

    public GithubReposMock() {
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw Throwables.propagate(e);
        }
    }

    @RequestMapping(method = GET)
    public ResponseEntity<GithubRepository> repositories(HttpServletRequest request,
                                                         @PathVariable("owner") String owner,
                                                         @PathVariable("repository") String repository) {
        requests.add(request);
        Optional<GithubRepository> userRepository = tryFindRepository(owner, repository);
        if (userRepository.isPresent()) {
            GithubRepository githubRepository = userRepository.get();
            return ResponseEntity
                    .status(HttpStatus.OK)
                    .cacheControl(CacheControl.maxAge(60, TimeUnit.SECONDS))
                    .eTag(eTagOf(githubRepository))
                    .body(githubRepository);
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }

    private String eTagOf(GithubRepository githubRepository) {
        return ETag.from(md5(reflectionToString(githubRepository, SHORT_PREFIX_STYLE))).toString();
    }

    private String md5(String str) {
        try {
            byte[] digest = md5.digest(str.getBytes(Charset.forName("UTF-8")));
            return DatatypeConverter.printHexBinary(digest).toUpperCase();
        } catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    private Optional<GithubRepository> tryFindRepository(String owner, String repositoryName) {
        return Optional.ofNullable(repositories.get(owner)).map(m -> m.get(repositoryName));
    }

    public void givenRepositories(GithubRepository... repositories) {
        givenRepositories(asList(repositories));
    }

    public void givenRepositories(List<GithubRepository> repoList) {
        this.repositories = new HashMap<>();
        for (GithubRepository repo : repoList) {
            this.repositories.computeIfAbsent(repo.getOwner().getLogin(), s -> new HashMap<>()).put(repo.getName(), repo);
        }
    }

    public List<HttpServletRequest> requests() {
        return ImmutableList.copyOf(requests);
    }

    public void reset() {
        repositories.clear();
        requests.clear();
    }
}

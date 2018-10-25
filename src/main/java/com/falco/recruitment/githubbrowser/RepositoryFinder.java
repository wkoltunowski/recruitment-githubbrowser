package com.falco.recruitment.githubbrowser;

import org.springframework.data.rest.webmvc.support.ETag;

import java.util.Optional;

public interface RepositoryFinder {
    Optional<Repository> tryFind(OwnerRepository ownerRepository, ETag eTag);
}

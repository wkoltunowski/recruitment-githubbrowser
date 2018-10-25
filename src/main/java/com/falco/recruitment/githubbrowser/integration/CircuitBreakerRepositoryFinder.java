package com.falco.recruitment.githubbrowser.integration;

import com.falco.recruitment.githubbrowser.OwnerRepository;
import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.RepositoryFinder;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service(CircuitBreakerRepositoryFinder.CIRCUIT_BREAKER_ID)
@EnableCircuitBreaker
@Slf4j
public class CircuitBreakerRepositoryFinder implements RepositoryFinder {
    public static final String CIRCUIT_BREAKER_ID = "circuitBreakerRepositoryFinder";
    public static final String COMMAND_KEY = "CircuitBreakerRepositoryFinder_KEY";
    @Autowired
    private GithubRepositoryFinder repositoryFinder;

    @Override
    @HystrixCommand(commandKey = COMMAND_KEY)
    public Optional<Repository> tryFind(OwnerRepository ownerRepository, ETag eTag) {
        LOG.trace("Using circuit breaker repository finder strategy for '{}', '{}'", ownerRepository, eTag);
        return repositoryFinder.tryFind(ownerRepository, eTag);
    }
}

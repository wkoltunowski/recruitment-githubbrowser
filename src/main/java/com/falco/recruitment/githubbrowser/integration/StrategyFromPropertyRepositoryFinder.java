package com.falco.recruitment.githubbrowser.integration;

import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.RepositoryFinder;
import com.falco.recruitment.githubbrowser.OwnerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.data.rest.webmvc.support.ETag;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Primary
@Slf4j
public class StrategyFromPropertyRepositoryFinder implements RepositoryFinder {
    @Value("${githubbrowser.repositoryFinderStrategy}")
    private String repositoryFinderStrategy;
    @Autowired
    private BeanFactory beanFactory;

    private RepositoryFinder repositoryFinder;

    @Override
    public Optional<Repository> tryFind(OwnerRepository ownerRepository, ETag eTag) {
        LOG.trace("Selecting '{}' repository finder strategy", repositoryFinderStrategy);
        RepositoryFinder finderStrategy = beanFactory.getBean(repositoryFinderStrategy, RepositoryFinder.class);
        LOG.trace("Found '{}' repository finder strategy", finderStrategy.getClass().getSimpleName());
        return finderStrategy.tryFind(ownerRepository, eTag);
    }
}

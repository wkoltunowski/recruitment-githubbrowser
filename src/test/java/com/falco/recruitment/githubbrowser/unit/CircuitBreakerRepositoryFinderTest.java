package com.falco.recruitment.githubbrowser.unit;

import com.falco.recruitment.githubbrowser.Repository;
import com.falco.recruitment.githubbrowser.RepositoryFinder;
import com.falco.recruitment.githubbrowser.integration.GithubRepositoryFinder;
import com.google.common.base.Throwables;
import com.jayway.awaitility.Awaitility;
import com.jayway.awaitility.core.ConditionFactory;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCircuitBreaker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static com.falco.recruitment.githubbrowser.integration.CircuitBreakerRepositoryFinder.CIRCUIT_BREAKER_ID;
import static com.falco.recruitment.githubbrowser.integration.CircuitBreakerRepositoryFinder.COMMAND_KEY;
import static com.netflix.config.ConfigurationManager.getConfigInstance;
import static com.netflix.hystrix.HystrixCommandKey.Factory.asKey;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CircuitBreakerRepositoryFinderTest {
    private static final Repository SUCCESSFUL_REPOSITORY = ownerRepository("successful.owner", "successful.repository");
    private static final Repository FAILING_REPOSITORY = ownerRepository("failing.owner", "failing.repository");
    private static final RuntimeException FAILING_EXCEPTION = new RuntimeException("Connection Lost!");

    @MockBean
    private GithubRepositoryFinder repositoryFinder;
    @Autowired
    @Qualifier(CIRCUIT_BREAKER_ID)
    private RepositoryFinder circuitBreakerRepositoryFinder;

    @Before
    public void init() {
        Hystrix.reset();
        setCommandProperty("circuitBreaker.requestVolumeThreshold", 1);
        setCommandProperty("circuitBreaker.sleepWindowInMilliseconds", 500);
    }

    @Test
    public void shouldFind() {
        givenRepository(SUCCESSFUL_REPOSITORY);
        assertThat(findViaCircuitBreaker(SUCCESSFUL_REPOSITORY)).isEqualTo(Optional.of(SUCCESSFUL_REPOSITORY));
        assertThat(getCircuitBreaker().isOpen()).isFalse();
    }

    @Test
    public void shouldTripCircuit() {
        givenRepository(SUCCESSFUL_REPOSITORY);
        findViaCircuitBreaker(SUCCESSFUL_REPOSITORY);

        when(repositoryFinder.tryFind(FAILING_REPOSITORY.ownerRepository(), FAILING_REPOSITORY.getETag())).thenThrow(FAILING_EXCEPTION);
        assertThatThrownBy(() -> findViaCircuitBreaker(FAILING_REPOSITORY)).isEqualTo(FAILING_EXCEPTION);
        await().until(() -> getCircuitBreaker().isOpen());
        assertThat(getCircuitBreaker().isOpen()).isTrue();
        assertThatThrownBy(() -> findViaCircuitBreaker(SUCCESSFUL_REPOSITORY)).isInstanceOf(RuntimeException.class);
    }

    private ConditionFactory await() {
        return Awaitility.await().atMost(1, TimeUnit.SECONDS);
    }

    @Test
    public void shouldCloseCircuit() {
        givenRepository(SUCCESSFUL_REPOSITORY);
        findViaCircuitBreaker(SUCCESSFUL_REPOSITORY);

        when(repositoryFinder.tryFind(FAILING_REPOSITORY.ownerRepository(), FAILING_REPOSITORY.getETag())).thenThrow(FAILING_EXCEPTION);
        assertThatThrownBy(() -> findViaCircuitBreaker(FAILING_REPOSITORY)).isEqualTo(FAILING_EXCEPTION);
        await().until(() -> getCircuitBreaker().isOpen());
        await().until(() -> checkCircuitClosed(SUCCESSFUL_REPOSITORY));
        findViaCircuitBreaker(SUCCESSFUL_REPOSITORY);
    }

    private Optional<Repository> findViaCircuitBreaker(Repository repository) {
        return circuitBreakerRepositoryFinder.tryFind(repository.ownerRepository(), repository.getETag());
    }

    private void givenRepository(Repository warmupRepository) {
        when(repositoryFinder.tryFind(warmupRepository.ownerRepository(), warmupRepository.getETag())).thenReturn(Optional.of(warmupRepository));
    }

    private Boolean checkCircuitClosed(Repository repository) {
        try {
            findViaCircuitBreaker(repository);
        } catch (Exception e) {
            if (!e.getMessage().equals("Hystrix circuit short-circuited and is OPEN")) {
                throw Throwables.propagate(e);
            }
        }
        return !getCircuitBreaker().isOpen();
    }

    public static HystrixCircuitBreaker getCircuitBreaker() {
        return HystrixCircuitBreaker.Factory.getInstance(asKey(COMMAND_KEY));
    }

    private void setCommandProperty(String optionName, int failedRequestsThreshold) {
        getConfigInstance().setProperty(String.format("hystrix.command.%s." + optionName, COMMAND_KEY), failedRequestsThreshold);
    }


    private static Repository ownerRepository(String owner, String repository) {
        return Repository.builder().owner(owner).name(repository).build();
    }
}
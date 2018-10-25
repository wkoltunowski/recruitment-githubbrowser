package com.falco.recruitment.githubbrowser.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class GithubRepository {
    @JsonProperty("full_name")
    private final String fullName;
    private final String description;
    @JsonProperty("clone_url")
    private final String cloneUrl;
    @JsonProperty("stargazers_count")
    private final int stars;
    @JsonProperty("created_at")
    private final LocalDateTime createdAt;
    @JsonProperty(value = "owner", required = true)
    private final Owner owner;
    @JsonProperty(required = true)
    private final String name;

    public static Owner login(String owner) {
        return new Owner(owner);
    }

    @Value
    public static class Owner {
        @JsonProperty(required = true)
        private final String login;
    }
}

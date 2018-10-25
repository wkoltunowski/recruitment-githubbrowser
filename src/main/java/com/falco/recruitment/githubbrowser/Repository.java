package com.falco.recruitment.githubbrowser;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.data.rest.webmvc.support.ETag;

import java.time.LocalDateTime;
import java.util.Optional;

@Value
@Builder
//needed for ignore properties to work
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.NONE)
public class Repository {
    private final String fullName;
    private final String description;
    private final String cloneUrl;
    private final int stars;
    private final LocalDateTime createdAt;
    @NonNull
    private final String owner;
    @NonNull
    private final String name;
    @JsonIgnore
    private final ETag eTag;

    public OwnerRepository ownerRepository() {
        return OwnerRepository.ownerRepository(owner, name);
    }

    public ETag getETag() {
        return Optional.ofNullable(eTag).orElse(ETag.NO_ETAG);
    }
}

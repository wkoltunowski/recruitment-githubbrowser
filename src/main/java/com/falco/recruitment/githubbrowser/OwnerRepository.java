package com.falco.recruitment.githubbrowser;

import lombok.NonNull;
import lombok.Value;

@Value
public class OwnerRepository {
    @NonNull
    private final String owner;
    @NonNull
    private final String repository;

    public static OwnerRepository ownerRepository(String owner, String repository) {
        return new OwnerRepository(owner, repository);
    }
}

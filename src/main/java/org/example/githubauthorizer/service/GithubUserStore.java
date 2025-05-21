package org.example.githubauthorizer.service;

import org.example.githubauthorizer.model.GithubUser;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GithubUserStore {
    private final Map<String, GithubUser> byId = new ConcurrentHashMap<>();

    public Optional<GithubUser> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }

    /**
     * Creates or overwrites the entry under the same GitHub numeric id.
     */
    public GithubUser save(GithubUser user) {
        byId.put(user.id(), user);
        return user;
    }
}

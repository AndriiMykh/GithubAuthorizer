package org.example.githubauthorizer.service;

import org.example.githubauthorizer.model.GithubUser;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
public class InMemoryUserStore {
    private final Map<String, GithubUser> users = new HashMap<>();

    public void save(GithubUser user)            { users.put(user.id(), user); }
    public Collection<GithubUser> findAll()   { return users.values(); }
}

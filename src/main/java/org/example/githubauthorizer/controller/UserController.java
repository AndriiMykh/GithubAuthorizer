package org.example.githubauthorizer.controller;

import org.example.githubauthorizer.model.GithubUser;
import org.example.githubauthorizer.service.InMemoryUserStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class UserController {
    private final InMemoryUserStore store;
    public UserController(InMemoryUserStore store) { this.store = store; }

    @GetMapping("/api/users")
    public Collection<GithubUser> list() {
        return store.findAll();
    }
}

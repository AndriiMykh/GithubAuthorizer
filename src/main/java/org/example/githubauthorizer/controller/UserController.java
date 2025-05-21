package org.example.githubauthorizer.controller;

import org.example.githubauthorizer.model.GithubUser;
import org.example.githubauthorizer.model.UserEntity;
import org.example.githubauthorizer.repository.UserRepository;
import org.example.githubauthorizer.service.InMemoryUserStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class UserController {
    private final UserRepository users;
    public UserController(UserRepository users) { this.users = users; }

    @GetMapping("/api/users")
    public Collection<UserEntity> list() {
        return users.findAll();
    }
}

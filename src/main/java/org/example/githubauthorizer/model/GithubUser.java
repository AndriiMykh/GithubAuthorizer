package org.example.githubauthorizer.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GithubUser(String id, String login, String avatarUrl, String name) {
}

package org.example.githubauthorizer;

import org.example.githubauthorizer.configuration.CustomGithubOAuthProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
public class GithubAuthorizerApplication {

    public static void main(String[] args) {
        SpringApplication.run(GithubAuthorizerApplication.class, args);
    }

}

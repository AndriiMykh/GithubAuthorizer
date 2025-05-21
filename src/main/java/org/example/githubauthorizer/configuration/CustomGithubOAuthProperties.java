package org.example.githubauthorizer.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "custom.github")
@Data
public class CustomGithubOAuthProperties  {
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String scopes;

}

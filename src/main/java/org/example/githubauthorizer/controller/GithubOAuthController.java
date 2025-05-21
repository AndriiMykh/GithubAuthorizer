package org.example.githubauthorizer.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.githubauthorizer.configuration.CustomGithubOAuthProperties;
import org.example.githubauthorizer.model.UserEntity;
import org.example.githubauthorizer.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers;

@Controller
public class GithubOAuthController {

    private final CustomGithubOAuthProperties githubProps;
    private final UserRepository users;
    private final ObjectMapper objectMapper;
    private final java.net.http.HttpClient httpClient;
    private final SecureRandom random;

    public GithubOAuthController(CustomGithubOAuthProperties githubProps, UserRepository users) {
        this.githubProps = githubProps;
        this.users = users;
        this.objectMapper = new ObjectMapper();
        this.httpClient = java.net.http.HttpClient.newHttpClient();
        this.random = new SecureRandom();
    }

    /**
     * Step 1: Redirect user to GitHub for login
     */
    @GetMapping("/login-github")
    public void login(HttpServletResponse res, HttpSession session) throws IOException {
        String state = HexFormat.of().withUpperCase().formatHex(random.generateSeed(16));
        session.setAttribute("oauthState", state); // CSRF protection

        String authorizeUrl = "https://github.com/login/oauth/authorize" +
                "?client_id=" + githubProps.getClientId() +
                "&redirect_uri=" + URLEncoder.encode(githubProps.getRedirectUri(), StandardCharsets.UTF_8) +
                "&scope=" + URLEncoder.encode(githubProps.getScopes().replace(' ', ','), StandardCharsets.UTF_8) +
                "&state=" + state;

        res.sendRedirect(authorizeUrl);
    }

    /**
     * Step 2: GitHub calls us back with ?code=…&state=…
     */
    @GetMapping("/oauth/github/callback")
    public ResponseEntity<?> callback(@RequestParam String code,
                                      @RequestParam String state,
                                      HttpSession session) throws Exception {

        // CSRF token check
        String expectedState = (String) session.getAttribute("oauthState");
        if (!state.equals(expectedState)) {
            return ResponseEntity.status(403).body("Invalid state");
        }

        // Step 3: Exchange code for access_token
        var tokenRequest = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Accept", "application/json")
                .POST(ofForm(Map.of(
                        "client_id", githubProps.getClientId(),
                        "client_secret", githubProps.getClientSecret(),
                        "code", code,
                        "redirect_uri", githubProps.getRedirectUri(),
                        "state", state
                )))
                .build();

        var tokenResponse = httpClient.send(tokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() != 200) {
            return ResponseEntity.status(500).body("Failed to obtain access token");
        }
        AccessToken at = objectMapper.readValue(tokenResponse.body(), AccessToken.class);

        // Step 4: Get user profile from GitHub
        var apiRequest = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user"))
                .header("Authorization", "Bearer " + at.accessToken())
                .header("Accept", "application/vnd.github+json")
                .build();

        var apiResponse = httpClient.send(apiRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        if (apiResponse.statusCode() != 200) {
            return ResponseEntity.status(500).body("Failed to fetch GitHub user profile");
        }
        Profile profile = objectMapper.readValue(apiResponse.body(), Profile.class);

        // Step 5: Save user if not present
        users.findByGithubId(profile.id)
                .orElseGet(() -> users.save(
                        UserEntity.builder()
                                .githubId(profile.id)
                                .login(profile.login)
                                .name(profile.name)
                                .avatarUrl(profile.avatarUrl)
                                .build()
                ));

        // Step 6: Authenticate user
        var principal = profile.login;
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        var ctx = SecurityContextHolder.createEmptyContext();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);

        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                ctx);

        return ResponseEntity.status(302).location(URI.create("/")).build();
    }

    private static java.net.http.HttpRequest.BodyPublisher ofForm(Map<String, String> data) {
        var sb = new StringBuilder();
        for (var entry : data.entrySet()) {
            if (!sb.isEmpty()) sb.append('&');
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
        }
        return BodyPublishers.ofString(sb.toString());
    }

    // --- DTOs ---
    public static record AccessToken(
            @JsonAlias("access_token") String accessToken,
            String scope,
            @JsonAlias("token_type") String tokenType
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Profile {
        public String id;
        public String login;
        public String name;
        @JsonAlias("avatar_url")
        public String avatarUrl;
    }
}

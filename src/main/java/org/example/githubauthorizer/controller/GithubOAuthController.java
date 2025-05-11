package org.example.githubauthorizer.controller;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.githubauthorizer.model.GithubUser;
import org.example.githubauthorizer.service.InMemoryUserStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
import java.util.Map;

import static java.net.http.HttpRequest.BodyPublishers;

@Controller
public class GithubOAuthController {
    @Value("${github.client-id}")     String clientId;
    @Value("${github.client-secret}") String clientSecret;
    @Value("${github.redirect-uri}")  String redirectUri;
    @Value("${github.scopes}")        String scopes;
    final ObjectMapper om = new ObjectMapper();
    final java.net.http.HttpClient http = java.net.http.HttpClient.newHttpClient();
    final SecureRandom random = new SecureRandom();
    private final InMemoryUserStore store;
    public GithubOAuthController(InMemoryUserStore store) { this.store = store; }
    /* 1/login‑github redirects user to GitHub */
    @GetMapping("/login-github")
    public void login(HttpServletResponse res, HttpSession session) throws IOException {
        String state = HexFormat.of().withUpperCase().formatHex(random.generateSeed(16));
        session.setAttribute("oauthState", state);       // CSRF protection

        String authorizeUrl = "https://github.com/login/oauth/authorize" +
                "?client_id="   + clientId +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                "&scope="       + URLEncoder.encode(scopes.replace(' ', ','), StandardCharsets.UTF_8) +
                "&state="       + state;

        res.sendRedirect(authorizeUrl);
    }

    /* 2️ GitHub calls us back with ?code=…&state=…  */
    @GetMapping("/oauth/github/callback")
    public ResponseEntity<?> callback(@RequestParam String code,
                                      @RequestParam String state,
                                      HttpSession session) throws Exception {

        // verify the CSRF token
        String expectedState = (String) session.getAttribute("oauthState");
        if (!state.equals(expectedState)) return ResponseEntity.status(403).body("Invalid state");

        // 3️ exchange code → access_token
        var tokenRequest = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://github.com/login/oauth/access_token"))
                .header("Accept", "application/json")
                .POST(ofForm(Map.of(
                        "client_id",     clientId,
                        "client_secret", clientSecret,
                        "code",          code,
                        "redirect_uri",  redirectUri,
                        "state",         state)))
                .build();

        var tokenResponse = http.send(tokenRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        AccessToken at = om.readValue(tokenResponse.body(), AccessToken.class);

        // 4️ call GitHub API with the token (example: /user profile)
        var apiReq = java.net.http.HttpRequest.newBuilder()
                .uri(URI.create("https://api.github.com/user"))
                .header("Authorization", "Bearer " + at.accessToken())
                .header("Accept", "application/vnd.github+json")
                .build();

        @JsonIgnoreProperties(ignoreUnknown = true)
        record Profile(String id, String login, String name,
                       @JsonAlias("avatar_url") String avatarUrl) {}
        var apiResp = http.send(apiReq, java.net.http.HttpResponse.BodyHandlers.ofString());
        Profile p = om.readValue(apiResp.body(), Profile.class);

        store.save(new GithubUser(p.id(), p.login(), p.avatarUrl(), p.name()));

        /*  redirect back to UI instead of dumping JSON  */
        return ResponseEntity.status(302)
                .location(URI.create("/"))
                .build();
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

    record AccessToken(
            @JsonAlias("access_token") String accessToken,
            String scope,
            @JsonAlias("token_type") String tokenType) {}
}

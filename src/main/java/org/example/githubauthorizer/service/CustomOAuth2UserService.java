package org.example.githubauthorizer.service;

import lombok.RequiredArgsConstructor;
import org.example.githubauthorizer.model.GithubUser;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    private final GithubUserStore users;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request)
            throws OAuth2AuthenticationException {
        System.out.println("Hello I'm here" );
        // 1. Let Spring call GitHub’s /user endpoint
        OAuth2User oauth2 = super.loadUser(request);
        Map<String, Object> a = oauth2.getAttributes();

        // 2. Map JSON → record fields
        String id        = String.valueOf(a.get("id"));          // numeric → string
        String login     = (String)  a.get("login");
        String avatarUrl = (String)  a.get("avatar_url");
        String name      = (String)  a.get("name");              // might be null

        // 3. Create-or-update in the map
        GithubUser local = new GithubUser(id, login, avatarUrl, name);
        users.save(local);   // id is the key; overwrites if already present

        // 4. Return the original principal (or wrap it if you need extra roles)
        return oauth2;
    }
}

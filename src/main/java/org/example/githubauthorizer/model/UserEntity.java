package org.example.githubauthorizer.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** GitHub numeric id – unique per GitHub account */
    @Column(name = "github_id", nullable = false, unique = true)
    private String githubId;

    private String login;
    private String name;

    @Column(name = "avatar_url")
    private String avatarUrl;
}

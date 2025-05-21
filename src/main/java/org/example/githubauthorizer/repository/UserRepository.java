package org.example.githubauthorizer.repository;

import org.example.githubauthorizer.model.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByGithubId(String githubId);
}


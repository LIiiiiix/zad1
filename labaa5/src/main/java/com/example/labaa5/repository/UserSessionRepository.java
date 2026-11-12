package com.example.labaa5.repository;

import com.example.labaa5.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findByRefreshTokenAndStatus(String refreshToken, com.example.labaa5.model.SessionStatus status);


    Optional<UserSession> findByRefreshToken(String refreshToken);
}
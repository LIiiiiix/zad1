package com.example.labaa5.service;

import com.example.labaa5.dto.TokenPairResponse;
import com.example.labaa5.model.SessionStatus;
import com.example.labaa5.model.UserSession;
import com.example.labaa5.repository.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TokenPairService {

    private final JwtService jwtService;
    private final UserSessionRepository sessionRepository;


    @Transactional
    public TokenPairResponse issueNewPair(String username, String deviceId) {
        Map<String, Object> extraClaims = new HashMap<>();
        if (deviceId != null) {
            extraClaims.put("device_id", deviceId);
        }

        String accessToken = jwtService.generateAccessToken(username, extraClaims);
        String refreshToken = jwtService.generateRefreshToken(username, extraClaims);



        Instant now = Instant.now();
        long accessExpMs = 900000L;
        long refreshExpMs = 86400000L;

        Instant accessExpiry = now.plusMillis(accessExpMs);
        Instant refreshExpiry = now.plusMillis(refreshExpMs);


        UserSession session = UserSession.builder()
                .username(username)
                .deviceId(deviceId)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiry(accessExpiry)
                .refreshTokenExpiry(refreshExpiry)
                .status(SessionStatus.ACTIVE)
                .build();


        sessionRepository.save(session);


        return TokenPairResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }


    @Transactional
    public TokenPairResponse refreshTokens(String oldRefreshToken) {
        UserSession existingSession = sessionRepository.findByRefreshToken(oldRefreshToken)
                .orElseThrow(() -> new IllegalArgumentException("Неизвестная сессия"));


        if (existingSession.getStatus() == SessionStatus.REVOKED) {
            throw new SecurityException("Сессия была отозвана");
        }

        if (existingSession.getStatus() == SessionStatus.USED) {
            existingSession.setStatus(SessionStatus.REVOKED);
            sessionRepository.save(existingSession);
            throw new SecurityException("Refresh token уже был использован (Possible Replay Attack)");
        }


        if (existingSession.getStatus() != SessionStatus.ACTIVE) {
            throw new SecurityException("Недопустимый статус сессии");
        }


        if (existingSession.getRefreshTokenExpiry().isBefore(Instant.now())) {
            existingSession.setStatus(SessionStatus.REVOKED);
            sessionRepository.save(existingSession);
            throw new SecurityException("Refresh token истек");
        }


        existingSession.setStatus(SessionStatus.USED);
        sessionRepository.save(existingSession);


        return issueNewPair(existingSession.getUsername(), existingSession.getDeviceId());
    }
}
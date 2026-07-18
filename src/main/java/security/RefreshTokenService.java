package security;

import entity.RefreshToken;
import lombok.AllArgsConstructor;
import repository.RefreshTokenRepository;
import repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
@AllArgsConstructor
public class RefreshTokenService { //READY
    // СОЗДАНИЕ REFRESH TOKEN
    // Вернуть RefreshToken, если он существует
    // Удалить RefreshToken, если его срок истёк
    // Удалить RefreshToken у Пользователя
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    //Возвращает RefreshToken, если он существует
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    //СОЗДАНИЕ REFRESH TOKEN
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();

        refreshToken.setUser(userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username)));

        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtProperties.getRefreshExpiration()));

        refreshToken.setToken(UUID.randomUUID().toString());

        return refreshTokenRepository.save(refreshToken);
    }

    //Удалить RefreshToken, если его срок истёк
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    //Удалить RefreshToken у Пользователя
    @Transactional
    public void deleteByUserId(String username) {
        userRepository.findByUsername(username).ifPresent(refreshTokenRepository::deleteByUser);
    }
}
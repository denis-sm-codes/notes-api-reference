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
public class RefreshTokenService {
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Создание нового рефреш токена для пользователя
    @Transactional
    public RefreshToken createRefreshToken(String username) {
        RefreshToken refreshToken = new RefreshToken();

        // Ищем пользователя, к которому привяжем токен
        refreshToken.setUser(userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found with username: " + username)));

        // Высчитываем дату протухания (текущий момент + 60 дней)
        refreshToken.setExpiryDate(Instant.now().plusMillis(jwtProperties.getRefreshExpiration()));

        // Генерируем уникальную UUID строку
        refreshToken.setToken(UUID.randomUUID().toString());

        // Сохраняем в базу данных PostgreSQL
        return refreshTokenRepository.save(refreshToken);
    }

    // Проверка: если токен протух — удаляем его из базы данных
    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new RuntimeException("Refresh token was expired. Please make a new signin request");
        }
        return token;
    }

    // Удаление сессий пользователя (например, при логауте)
    @Transactional
    public void deleteByUserId(String username) {
        userRepository.findByUsername(username).ifPresent(refreshTokenRepository::deleteByUser);
    }
}
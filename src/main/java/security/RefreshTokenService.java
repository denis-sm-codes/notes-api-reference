package security;

import entity.RefreshToken;
import repository.RefreshTokenRepository;
import repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {

    // Читаем те самые 60 дней из нашего JwtUtil конфигурационного пула
    @Value("${JWT_REFRESH_EXPIRATION:5184000000}")
    private long refreshTokenExpirationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

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
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationMs));

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
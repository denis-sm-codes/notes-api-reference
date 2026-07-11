package security;

import dto.AuthRequest;
import dto.AuthResponse;
import dto.RegisterRequest;
import entity.Role;
import entity.User;
import entity.RefreshToken;
import lombok.AllArgsConstructor;
import repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    // 1. РЕГИСТРАЦИЯ ПОЛЬЗОВАТЕЛЯ
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Проверяем, не занято ли имя пользователя
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Error: Username is already taken!");
        }

        // Проверяем, не занята ли почта
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Error: Email is already in use!");
        }

        // Создаем нового пользователя и хэшируем пароль
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER) // По дефолту даем роль обычного пользователя
                .build();

        userRepository.save(user);

        // Чтобы сгенерировать JWT, нам нужен объект UserDetails
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(Role.ROLE_USER.name())
                .build();

        // Штампуем оба токена
        String accessToken = jwtUtil.generateToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(user.getUsername())
                .build();
    }

    // 2. ВХОД В СИСТЕМУ (ЛОГИН)
    @Transactional
    public AuthResponse login(AuthRequest request) {
        // Проверяем логин и пароль стандартными средствами Спринга
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();

        // Генерируем новый Access-токен
        String accessToken = jwtUtil.generateToken(userDetails);

        // При каждом логине создаем свежий Refresh-токен (старые можно почистить)
        refreshTokenService.deleteByUserId(userDetails.getUsername());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(userDetails.getUsername());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .username(userDetails.getUsername())
                .build();
    }

    // 3. ОБНОВЛЕНИЕ ACCESS-ТОКЕНА ЧЕРЕЗ REFRESH
    @Transactional
    public AuthResponse refreshToken(String requestRefreshToken) {
        return refreshTokenService.findByToken(requestRefreshToken)
                // Проверяем, не протух ли токен в БД по времени
                .map(refreshTokenService::verifyExpiration)
                // Достаем пользователя, которому принадлежит токен
                .map(RefreshToken::getUser)
                .map(user -> {
                    // Формируем UserDetails для генерации нового JWT
                    UserDetails userDetails = org.springframework.security.core.userdetails.User
                            .withUsername(user.getUsername())
                            .password(user.getPassword())
                            .authorities(user.getRole().name())
                            .build();

                    // Генерируем только новый Access-токен на 15 минут
                    String accessToken = jwtUtil.generateToken(userDetails);

                    // Возвращаем старый рефреш и новый аксцесс
                    return AuthResponse.builder()
                            .accessToken(accessToken)
                            .refreshToken(requestRefreshToken)
                            .username(user.getUsername())
                            .build();
                })
                .orElseThrow(() -> new RuntimeException("Refresh token is not in database!"));
    }
}
package security;

import dto.request.AuthRequest;
import dto.request.RegisterRequest;
import dto.response.AuthResponse;
import entity.RefreshToken;
import entity.Role;
import entity.User;
import exception.TokenRefreshException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import repository.RefreshTokenRepository;
import repository.UserRepository;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("denis")
                .email("denis@example.com")
                .password("encoded_password")
                .role(Role.ROLE_USER)
                .build();

        testRefreshToken = new RefreshToken();
        testRefreshToken.setId(1L);
        testRefreshToken.setToken("refresh-token-123");
        testRefreshToken.setUser(testUser);
        testRefreshToken.setExpiryDate(Instant.now().plusSeconds(3600));
    }

    @Nested
    @DisplayName("Тесты метода register()")
    class RegisterTests {

        @Test
        @DisplayName("Успешная регистрация нового пользователя")
        void register_Success() {
            RegisterRequest request = new RegisterRequest("denis", "denis@example.com", "password123");

            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userRepository.existsByEmail(request.email())).thenReturn(false);
            when(passwordEncoder.encode(request.password())).thenReturn("encoded_password");
            when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("access-token-123");
            when(refreshTokenService.createRefreshToken("denis")).thenReturn(testRefreshToken);

            AuthResponse response = authService.register(request);

            assertNotNull(response);
            assertEquals("access-token-123", response.accessToken());
            assertEquals("refresh-token-123", response.refreshToken());
            assertEquals("denis", response.username());

            verify(userRepository, times(1)).save(any(User.class));
        }

        @Test
        @DisplayName("Ошибка: Username уже занят")
        void register_UsernameTaken_ThrowsException() {
            RegisterRequest request = new RegisterRequest("denis", "denis@example.com", "password123");

            when(userRepository.existsByUsername(request.username())).thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.register(request));

            assertEquals("Error: Username is already taken!", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }

        @Test
        @DisplayName("Ошибка: Email уже используется")
        void register_EmailInUse_ThrowsException() {
            RegisterRequest request = new RegisterRequest("denis", "denis@example.com", "password123");

            when(userRepository.existsByUsername(request.username())).thenReturn(false);
            when(userRepository.existsByEmail(request.email())).thenReturn(true);

            RuntimeException exception = assertThrows(RuntimeException.class,
                    () -> authService.register(request));

            assertEquals("Error: Email is already in use!", exception.getMessage());
            verify(userRepository, never()).save(any(User.class));
        }
    }

    @Nested
    @DisplayName("Тесты метода login()")
    class LoginTests {

        @Test
        @DisplayName("Успешный вход в аккаунт")
        void login_Success() {
            AuthRequest request = new AuthRequest("denis", "password123");
            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(testUser);
            when(jwtUtil.generateToken(any(UserDetails.class))).thenReturn("access-token-123");
            when(refreshTokenService.createRefreshToken("denis")).thenReturn(testRefreshToken);

            AuthResponse response = authService.login(request);

            assertNotNull(response);
            assertEquals(1L, response.id());
            assertEquals("denis", response.username());
            assertEquals("access-token-123", response.accessToken());
            assertEquals("refresh-token-123", response.refreshToken());

            verify(refreshTokenService, times(1)).deleteByUser(testUser);
            verify(refreshTokenService, times(1)).createRefreshToken("denis");
        }
    }

    @Nested
    @DisplayName("Тесты метода updateTokens()")
    class UpdateTokensTests {

        @Test
        @DisplayName("Успешное обновление токенов")
        void updateTokens_Success() {
            String requestToken = "refresh-token-123";
            UserDetails userDetails = org.springframework.security.core.userdetails.User
                    .withUsername("denis")
                    .password("encoded_password")
                    .authorities("ROLE_USER")
                    .build();

            when(refreshTokenService.findByToken(requestToken)).thenReturn(Optional.of(testRefreshToken));
            when(refreshTokenService.verifyExpiration(testRefreshToken)).thenReturn(testRefreshToken);
            when(refreshTokenService.createRefreshToken("denis")).thenReturn(testRefreshToken);
            when(userDetailsService.loadUserByUsername("denis")).thenReturn(userDetails);
            when(jwtUtil.generateToken(userDetails)).thenReturn("new-access-token");

            AuthResponse response = authService.updateTokens(requestToken);

            assertNotNull(response);
            assertEquals("new-access-token", response.accessToken());
            assertEquals("refresh-token-123", response.refreshToken());
            assertEquals("denis", response.username());

            verify(refreshTokenService, times(1)).deleteByUserId(1L);
        }

        @Test
        @DisplayName("Ошибка: Refresh token не найден в БД")
        void updateTokens_TokenNotFound_ThrowsException() {
            String requestToken = "invalid-token";

            when(refreshTokenService.findByToken(requestToken)).thenReturn(Optional.empty());

            assertThrows(TokenRefreshException.class,
                    () -> authService.updateTokens(requestToken));
        }
    }

    @Nested
    @DisplayName("Тесты метода logout()")
    class LogoutTests {

        @Test
        @DisplayName("Успешный выход из системы (Logout)")
        void logout_Success() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("denis");
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findByUsername("denis")).thenReturn(Optional.of(testUser));

            authService.logout();

            verify(refreshTokenRepository, times(1)).deleteByUser(testUser);

            SecurityContextHolder.clearContext();
        }

        @Test
        @DisplayName("Ошибка Logout: Пользователь из контекста не найден в БД")
        void logout_UserNotFound_ThrowsException() {
            Authentication authentication = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);

            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("unknown");
            SecurityContextHolder.setContext(securityContext);

            when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThrows(UsernameNotFoundException.class, () -> authService.logout());

            verify(refreshTokenRepository, never()).deleteByUser(any());

            SecurityContextHolder.clearContext();
        }
    }
}
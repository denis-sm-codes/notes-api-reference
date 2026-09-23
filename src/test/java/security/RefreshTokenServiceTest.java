package security;

import entity.RefreshToken;
import entity.User;
import repository.RefreshTokenRepository;
import repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private JwtProperties jwtProperties;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User testUser;
    private RefreshToken validToken;
    private RefreshToken expiredToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("denis");

        validToken = new RefreshToken();
        validToken.setId(10L);
        validToken.setToken("valid-uuid-token");
        validToken.setUser(testUser);
        validToken.setExpiryDate(Instant.now().plusSeconds(3600));

        expiredToken = new RefreshToken();
        expiredToken.setId(11L);
        expiredToken.setToken("expired-uuid-token");
        expiredToken.setUser(testUser);
        expiredToken.setExpiryDate(Instant.now().minusSeconds(3600));
    }

    @Test
    void findByToken_WhenTokenExists_ReturnsOptionalToken() {
        when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));

        Optional<RefreshToken> result = refreshTokenService.findByToken("valid-uuid-token");

        assertTrue(result.isPresent());
        assertEquals("valid-uuid-token", result.get().getToken());
    }

    @Test
    void createRefreshToken_Success() {
        when(userRepository.findByUsername("denis")).thenReturn(Optional.of(testUser));
        when(jwtProperties.getRefreshExpiration()).thenReturn(86400000L); // 1 день
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken createdToken = refreshTokenService.createRefreshToken("denis");

        assertNotNull(createdToken);
        assertNotNull(createdToken.getToken());
        assertEquals(testUser, createdToken.getUser());
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void createRefreshToken_WhenUserNotFound_ThrowsException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.createRefreshToken("unknown"));

        assertEquals("User not found with username: unknown", exception.getMessage());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void verifyExpiration_WhenValid_ReturnsToken() {
        RefreshToken result = refreshTokenService.verifyExpiration(validToken);

        assertNotNull(result);
        assertEquals(validToken, result);
        verify(refreshTokenRepository, never()).delete(any());
    }

    @Test
    void verifyExpiration_WhenExpired_ThrowsExceptionAndDeletes() {
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> refreshTokenService.verifyExpiration(expiredToken));

        assertEquals("Refresh token was expired. Please make a new signin request", exception.getMessage());
        verify(refreshTokenRepository, times(1)).delete(expiredToken);
    }

    @Test
    void deleteByUser_Success() {
        refreshTokenService.deleteByUser(testUser);

        verify(refreshTokenRepository, times(1)).deleteByUser(testUser);
    }

    @Test
    void deleteByUsername_Success() {
        refreshTokenService.deleteByUsername("denis");

        verify(refreshTokenRepository, times(1)).deleteByUsername("denis");
    }

    @Test
    void deleteByUserId_Success() {
        refreshTokenService.deleteByUserId(1L);

        verify(refreshTokenRepository, times(1)).deleteByUserId(1L);
    }
}